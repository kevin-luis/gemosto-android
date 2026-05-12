package com.gemosto.feature.exercise

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.firestore.ExerciseRepository
import com.gemosto.data.firestore.PainLogRepository
import com.gemosto.data.firestore.ProfileRepository
import com.gemosto.data.firestore.RomRepository
import com.gemosto.data.llm.GeminiNarrativeService
import com.gemosto.data.llm.NarrativeInput
import com.gemosto.domain.exercise.ExerciseRuleEngine
import com.gemosto.domain.model.AuthState
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.ProgramStatus
import com.gemosto.domain.exercise.PainLogLogic
import com.gemosto.domain.model.PainEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel untuk Program & Exercise feature (spec 003).
 *
 * Tanggung jawab:
 *  - Observe active program (real-time dari Firestore)
 *  - Generate program: rule engine compute → save skeleton → Gemini call paralel → update narrative
 *  - Trigger archive program lama saat regenerate
 *
 * Strategi save bertahap:
 *  1. Rule engine compute (instant)
 *  2. Save skeleton program ke Firestore (status=ACTIVE, narrative=null)
 *  3. UI bisa langsung tampil (latihan + safety note)
 *  4. Gemini call paralel di background — update narrative saat sukses/fallback
 *
 * Spec: 003-exercise-recommendation.md section 7.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository,
    private val romRepo: RomRepository,
    private val exerciseRepo: ExerciseRepository,
    private val painLogRepo: PainLogRepository,
    private val ruleEngine: ExerciseRuleEngine,
    private val narrativeService: GeminiNarrativeService,
) : ViewModel() {

    private val _genState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _genState.asStateFlow()

    /**
     * Active program reactive dari Firestore.
     *
     * Loading dipisah dari Loaded(null) supaya UI tidak menampilkan empty state
     * sebelum snapshot pertama selesai dibaca.
     */
    val activeProgramState: StateFlow<ActiveProgramState> = authRepo.authState
        .flatMapLatest { auth ->
            when (auth) {
                is AuthState.SignedIn -> exerciseRepo.observeActive(auth.uid)
                    .map<ExerciseProgram?, ActiveProgramState> { ActiveProgramState.Loaded(it) }
                    .onStart { emit(ActiveProgramState.Loading) }
                    .catch { e ->
                        Log.w(TAG, "Failed to observe active program", e)
                        emit(ActiveProgramState.Loaded(null))
                    }
                AuthState.Loading -> flowOf(ActiveProgramState.Loading)
                AuthState.SignedOut -> flowOf(ActiveProgramState.Loaded(null))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ActiveProgramState.Loading,
        )

    /**
     * Convenience value untuk logic internal yang hanya butuh programnya.
     */
    val activeProgram: StateFlow<ExerciseProgram?> = activeProgramState
        .map { state -> (state as? ActiveProgramState.Loaded)?.program }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /**
     * Generate program berdasarkan ROM result terakhir + profile user.
     * @param romId optional — kalau null, ambil ROM terbaru
     */
    fun generateProgram(romId: String? = null) {
        if (_genState.value is GenerateState.Generating) return
        _genState.value = GenerateState.Generating

        viewModelScope.launch {
            try {
                val uid = authRepo.currentUid()
                    ?: return@launch failGenerate("Sesi login expired. Silakan login ulang.")

                val profile = profileRepo.get(uid)
                    ?: return@launch failGenerate("Profil tidak ditemukan. Lengkapi profil Anda.")

                // Pakai ROM terbaru kalau romId tidak diberikan
                val rom = if (romId != null) {
                    romRepo.get(uid, romId)
                } else {
                    // observeLatest as one-shot via Flow.first() — simplified MVP
                    var latest: com.gemosto.domain.model.RomResult? = null
                    romRepo.observeLatest(uid).collect {
                        latest = it
                        return@collect
                    }
                    latest
                }

                if (rom == null) {
                    return@launch failGenerate(
                        "Belum ada hasil scan ROM. Lakukan Scan ROM dulu di tab Scan.",
                    )
                }

                // Archive program lama (kalau ada) — supaya hanya 1 ACTIVE
                activeProgram.value?.let { old ->
                    Log.d(TAG, "Archiving old program ${old.id}")
                    exerciseRepo.archive(uid, old.id)
                }
                
                // Fetch recent pain logs (limit 10 for safety check)
                val recentPainLogs = painLogRepo.recentEntries(10).firstOrNull() ?: emptyList()
                val recentPainScore = recentPainLogs.firstOrNull()?.score
                val consecutiveHighPain = PainLogLogic.computeConsecutiveHighPainCount(recentPainLogs)

                // STEP 1 — Rule engine compute (instant)
                val ruleInput = ExerciseRuleEngine.Input(
                    romCategory = rom.category,
                    age = profile.age,
                    activityLevel = profile.activityLevel,
                    recentPainScore = recentPainScore,
                    consecutiveHighPainCount = consecutiveHighPain,
                )
                val ruleOutput = ruleEngine.compute(ruleInput)
                Log.d(TAG, "RuleEngine output: level=${ruleOutput.level}, ${ruleOutput.exercises.size} exercises")

                // STEP 2 — Save skeleton program (narrative = null)
                val nowMs = System.currentTimeMillis()
                val programId = "prog_${nowMs}_${UUID.randomUUID().toString().take(8)}"
                val status = if (ruleOutput.level == ExerciseLevel.BLOCK) {
                    ProgramStatus.BLOCKED_PAIN
                } else {
                    ProgramStatus.ACTIVE
                }
                val program = ExerciseProgram(
                    id = programId,
                    userId = uid,
                    generatedAt = nowMs,
                    basedOnRomId = rom.id,
                    romCategory = rom.category,
                    level = ruleOutput.level,
                    durationWeeks = ruleOutput.durationWeeks,
                    frequencyPerWeek = ruleOutput.frequencyPerWeek,
                    exercises = ruleOutput.exercises,
                    narrative = null,
                    status = status,
                    safetyNote = ruleOutput.safetyNote,
                )

                val saveResult = exerciseRepo.upsert(program)
                if (saveResult.isFailure) {
                    val err = saveResult.exceptionOrNull()
                    Log.e(TAG, "Failed to save program skeleton", err)
                    return@launch failGenerate(
                        "Gagal menyimpan program: ${err?.message ?: "—"}",
                    )
                }

                // UI sekarang bisa render program (via Firestore listener)
                _genState.value = GenerateState.Success(programId)

                // STEP 3 — Gemini call paralel (kalau program tidak BLOCK)
                if (ruleOutput.level != ExerciseLevel.BLOCK) {
                    launchGemini(
                        uid = uid,
                        programId = programId,
                        age = profile.age,
                        kneeSide = rom.kneeSide,
                        romCategory = rom.category,
                        ruleOutput = ruleOutput,
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "generateProgram unexpected error", e)
                failGenerate("Error tak terduga: ${e.message ?: "—"}")
            }
        }
    }

    /**
     * Call Gemini di background — kalau sukses update narrative di Firestore;
     * kalau Gemini gagal/timeout, fallback statis di-update juga supaya UI
     * tetap punya narrative final (bukan stuck null).
     */
    private fun launchGemini(
        uid: String,
        programId: String,
        age: Int,
        kneeSide: KneeSide,
        romCategory: com.gemosto.domain.model.RomCategory,
        ruleOutput: ExerciseRuleEngine.Output,
    ) {
        viewModelScope.launch {
            val narrative = narrativeService.generate(
                NarrativeInput(
                    romCategory = romCategory,
                    age = age,
                    kneeSide = kneeSide,
                    level = ruleOutput.level,
                    exercises = ruleOutput.exercises,
                    durationWeeks = ruleOutput.durationWeeks,
                    frequencyPerWeek = ruleOutput.frequencyPerWeek,
                )
            )
            val updateResult = exerciseRepo.updateNarrative(uid, programId, narrative)
            if (updateResult.isFailure) {
                Log.w(TAG, "Failed to update narrative in Firestore", updateResult.exceptionOrNull())
            } else {
                Log.d(TAG, "Narrative updated (source=${narrative.source})")
            }
        }
    }

    fun consumeGenerateState() {
        _genState.value = GenerateState.Idle
    }

    private fun failGenerate(message: String) {
        Log.w(TAG, "Generate failed: $message")
        _genState.value = GenerateState.Failed(message)
    }

    fun savePainLog(score: Int, stoppedDueToPain: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepo.currentUid()
            if (uid == null) {
                onComplete()
                return@launch
            }
            
            val entry = PainEntry(
                id = "pain_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
                userId = uid,
                timestampMs = System.currentTimeMillis(),
                score = score,
                stoppedDueToPain = stoppedDueToPain,
                programId = activeProgram.value?.id,
                sessionStartedAtMs = null, // Can add tracking later if needed
                sessionDurationMs = null
            )
            
            val result = painLogRepo.save(entry)
            if (result.isFailure) {
                Log.e(TAG, "Failed to save pain log", result.exceptionOrNull())
            }
            onComplete()
        }
    }

    companion object {
        private const val TAG = "ProgramVM"
    }
}

/**
 * State machine untuk flow generate program.
 */
sealed interface GenerateState {
    data object Idle : GenerateState
    data object Generating : GenerateState
    data class Success(val programId: String) : GenerateState
    data class Failed(val message: String) : GenerateState
}

/**
 * State load program aktif.
 */
sealed interface ActiveProgramState {
    data object Loading : ActiveProgramState
    data class Loaded(val program: ExerciseProgram?) : ActiveProgramState
}
