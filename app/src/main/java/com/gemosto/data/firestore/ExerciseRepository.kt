package com.gemosto.data.firestore

import com.gemosto.domain.exercise.Exercise
import com.gemosto.domain.exercise.ExerciseCatalog
import com.gemosto.domain.exercise.ExerciseId
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseNarrative
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.NarrativeSource
import com.gemosto.domain.model.ProgramStatus
import com.gemosto.domain.model.RomCategory
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk Exercise Programs. Path: `users/{uid}/programs/{programId}`.
 *
 * Schema Firestore (per dokumen):
 * - id, userId, generatedAt, basedOnRomId
 * - romCategory, level (String enum name)
 * - durationWeeks, frequencyPerWeek
 * - exercises: List<Map<String, Any>> (id, sets, reps — minimum subset)
 * - narrative: Map<String, Any?>? (intro, rationale, weeklyMotivation, source)
 * - status, safetyNote
 *
 * Catalog content (description, tips, warnings) TIDAK disimpan di Firestore —
 * di-resolve client-side dari [ExerciseCatalog] berdasar `id`.
 */
interface ExerciseRepository {

    fun observeActive(uid: String): Flow<ExerciseProgram?>

    suspend fun get(uid: String, programId: String): ExerciseProgram?

    suspend fun upsert(program: ExerciseProgram): Result<Unit>

    /**
     * Update narrative aja — dipakai setelah Gemini call selesai.
     * Hemat write quota dibanding upsert full document.
     */
    suspend fun updateNarrative(
        uid: String,
        programId: String,
        narrative: ExerciseNarrative,
    ): Result<Unit>

    /** Archive program (set status = ARCHIVED). Dipakai saat user regenerate. */
    suspend fun archive(uid: String, programId: String): Result<Unit>

    suspend fun deleteAll(uid: String): Result<Unit>
}

class FirestoreExerciseRepository(
    private val firestore: FirebaseFirestore,
) : ExerciseRepository {

    private fun collection(uid: String) = firestore
        .collection(USERS).document(uid)
        .collection(PROGRAMS)

    override fun observeActive(uid: String): Flow<ExerciseProgram?> = callbackFlow {
        val sub = collection(uid)
            .whereIn("status", listOf(ProgramStatus.ACTIVE.name, ProgramStatus.BLOCKED_PAIN.name))
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.firstOrNull()?.toExerciseProgram())
            }
        awaitClose { sub.remove() }
    }.catch { it.printStackTrace(); emit(null) }

    override suspend fun get(uid: String, programId: String): ExerciseProgram? {
        return collection(uid).document(programId).get().await().toExerciseProgram()
    }

    override suspend fun upsert(program: ExerciseProgram): Result<Unit> = runCatching {
        collection(program.userId).document(program.id)
            .set(program.toFirestoreMap())
            .await()
        Unit
    }

    override suspend fun updateNarrative(
        uid: String,
        programId: String,
        narrative: ExerciseNarrative,
    ): Result<Unit> = runCatching {
        collection(uid).document(programId)
            .update("narrative", narrative.toFirestoreMap())
            .await()
        Unit
    }

    override suspend fun archive(uid: String, programId: String): Result<Unit> = runCatching {
        collection(uid).document(programId)
            .update("status", ProgramStatus.ARCHIVED.name)
            .await()
        Unit
    }

    override suspend fun deleteAll(uid: String): Result<Unit> = runCatching {
        val snapshot = collection(uid).get().await()
        if (snapshot.isEmpty) return@runCatching Unit
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Unit
    }

    companion object {
        private const val USERS = "users"
        private const val PROGRAMS = "programs"
    }
}

// ─── Mapping ─────────────────────────────────────────────────────

private fun ExerciseProgram.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "generatedAt" to generatedAt,
    "basedOnRomId" to basedOnRomId,
    "romCategory" to romCategory.name,
    "level" to level.name,
    "durationWeeks" to durationWeeks,
    "frequencyPerWeek" to frequencyPerWeek,
    "exercises" to exercises.map { it.toFirestoreMap() },
    "narrative" to narrative?.toFirestoreMap(),
    "status" to status.name,
    "safetyNote" to safetyNote,
)

private fun Exercise.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.name,
    "sets" to sets,
    "reps" to reps,
    "restSeconds" to restSeconds,
)

private fun ExerciseNarrative.toFirestoreMap(): Map<String, Any?> = mapOf(
    "intro" to intro,
    "rationale" to rationale,
    "weeklyMotivation" to weeklyMotivation,
    "source" to source.name,
)

private fun DocumentSnapshot.toExerciseProgram(): ExerciseProgram? {
    if (!exists()) return null
    return try {
        // Resolve exercises dari list of map
        @Suppress("UNCHECKED_CAST")
        val rawExercises = get("exercises") as? List<Map<String, Any?>> ?: emptyList()
        val exercises = rawExercises.mapNotNull { it.toExerciseFromCatalog() }

        @Suppress("UNCHECKED_CAST")
        val rawNarrative = get("narrative") as? Map<String, Any?>
        val narrative = rawNarrative?.toExerciseNarrative()

        ExerciseProgram(
            id = getString("id") ?: id,
            userId = getString("userId").orEmpty(),
            generatedAt = getLong("generatedAt") ?: return null,
            basedOnRomId = getString("basedOnRomId").orEmpty(),
            romCategory = RomCategory.valueOf(getString("romCategory") ?: return null),
            level = ExerciseLevel.valueOf(getString("level") ?: return null),
            durationWeeks = getLong("durationWeeks")?.toInt() ?: 4,
            frequencyPerWeek = getString("frequencyPerWeek").orEmpty(),
            exercises = exercises,
            narrative = narrative,
            status = ProgramStatus.valueOf(getString("status") ?: return null),
            safetyNote = getString("safetyNote"),
        )
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun Map<String, Any?>.toExerciseFromCatalog(): Exercise? {
    val idStr = this["id"] as? String ?: return null
    val id = try {
        ExerciseId.valueOf(idStr)
    } catch (e: IllegalArgumentException) {
        return null
    }
    val base = ExerciseCatalog.get(id)
    val sets = (this["sets"] as? Long)?.toInt() ?: base.sets
    val reps = (this["reps"] as? Long)?.toInt() ?: base.reps
    val restSeconds = (this["restSeconds"] as? Long)?.toInt() ?: base.restSeconds
    return base.copy(sets = sets, reps = reps, restSeconds = restSeconds)
}

private fun Map<String, Any?>.toExerciseNarrative(): ExerciseNarrative? {
    val intro = this["intro"] as? String ?: return null
    val rationale = this["rationale"] as? String ?: return null
    val motiv = this["weeklyMotivation"] as? String ?: return null
    val sourceStr = this["source"] as? String ?: NarrativeSource.FALLBACK_STATIC.name
    val source = try {
        NarrativeSource.valueOf(sourceStr)
    } catch (e: IllegalArgumentException) {
        NarrativeSource.FALLBACK_STATIC
    }
    return ExerciseNarrative(intro, rationale, motiv, source)
}
