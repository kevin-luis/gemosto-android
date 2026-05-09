package com.gemosto.feature.scan

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.firestore.RomRepository
import com.gemosto.data.pose.PoseDetection
import com.gemosto.data.pose.PoseDetector
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.RomCategory
import com.gemosto.domain.model.RomResult
import com.gemosto.domain.rom.Percentile
import com.gemosto.domain.rom.RomAngleCalculator
import com.gemosto.domain.rom.RomClassifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * State holder untuk flow Scan ROM.
 *
 * Tanggung jawab:
 *  - Pre-fill sisi lutut dari profile (Hari 5)
 *  - Stream pose detection → real-time angle (Hari 6)
 *  - 2-phase session state machine (Hari 8)
 *  - Sample collection + percentile finalize (Hari 8)
 *  - Save RomResult ke Firestore (Hari 8)
 */
class ScanViewModel(
    private val poseDetector: PoseDetector,
    private val authRepo: AuthRepository,
    private val romRepo: RomRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val flexionSamples = mutableListOf<Float>()
    private val extensionLagSamples = mutableListOf<Float>()
    private var sessionStartedAtMs: Long = 0L
    private var sessionJob: Job? = null
    private var poseJob: Job? = null

    fun initFromProfile(affected: KneeSide) {
        if (_state.value.selectedKneeSide != null) return
        _state.update {
            it.copy(
                selectedKneeSide = when (affected) {
                    KneeSide.LEFT -> KneeSide.LEFT
                    KneeSide.RIGHT -> KneeSide.RIGHT
                    KneeSide.BOTH -> KneeSide.RIGHT
                },
            )
        }
    }

    fun onKneeSelected(side: KneeSide) {
        if (side == KneeSide.BOTH) return
        _state.update { it.copy(selectedKneeSide = side) }
    }

    /**
     * Mulai mengkonsumsi pose detection stream — dipanggil saat enter Camera.
     * Otomatis berhenti saat ViewModel cleared.
     */
    fun startObservingPose() {
        if (poseJob?.isActive == true) return
        poseJob = viewModelScope.launch {
            poseDetector.detections.collect { processPose(it) }
        }
    }

    /**
     * Reset session state — dipanggil saat user keluar Camera tanpa selesaikan
     * pengukuran (back / close button).
     */
    fun resetSession() {
        sessionJob?.cancel()
        sessionJob = null
        flexionSamples.clear()
        extensionLagSamples.clear()
        _state.update {
            it.copy(
                phase = SessionPhase.IDLE,
                phaseRemainingSeconds = 0,
                sessionErrorMessage = null,
                completedRomId = null,
                completedRom = null,
            )
        }
    }

    /**
     * Mulai 2-phase measurement: countdown → extension → countdown → flexion → save.
     */
    fun startSession() {
        if (_state.value.phase != SessionPhase.IDLE) return
        flexionSamples.clear()
        extensionLagSamples.clear()
        sessionStartedAtMs = System.currentTimeMillis()

        sessionJob = viewModelScope.launch {
            try {
                runPhase(SessionPhase.COUNTDOWN_EXT, 3)
                runSamplingPhase(SessionPhase.EXTENSION) { extensionLagSamples.size }
                runPhase(SessionPhase.COUNTDOWN_FLEX, 3)
                runSamplingPhase(SessionPhase.FLEXION) { flexionSamples.size }
                finalize()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // User cancel — state sudah di-reset di resetSession()
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Session failed", e)
                _state.update {
                    it.copy(
                        phase = SessionPhase.FAILED,
                        sessionErrorMessage = "Sesi pengukuran gagal: ${e.message ?: "—"}",
                    )
                }
            }
        }
    }

    /**
     * Fixed-duration phase (untuk countdown). Hitung mundur per detik.
     */
    private suspend fun runPhase(phase: SessionPhase, durationSec: Int) {
        Log.d(TAG, "Phase enter: $phase ($durationSec s)")
        var remaining = durationSec
        _state.update { it.copy(phase = phase, phaseRemainingSeconds = remaining) }
        while (remaining > 0) {
            delay(1000)
            remaining--
            _state.update { it.copy(phaseRemainingSeconds = remaining) }
        }
        Log.d(TAG, "Phase exit: $phase. ext=${extensionLagSamples.size}, flex=${flexionSamples.size}")
    }

    /**
     * Adaptive sampling phase — adjust ke kapabilitas device.
     *
     * Aturan:
     *  - Tunggu minimum [MIN_PHASE_DURATION_SEC] detik (UX: jangan pendek)
     *  - Setelah min duration tercapai, exit kalau samples ≥ [MIN_SAMPLES_PER_PHASE]
     *  - Maksimum [MAX_PHASE_DURATION_SEC] detik (kalau device terlalu slow,
     *    paksa keluar lalu finalize akan fail kalau samples masih kurang)
     *
     * Trade-off: device cepat (30+ fps) selesai di 5 detik (UX cepat).
     * Device lambat (<3 fps) dapat tambahan waktu sampai 10 detik.
     */
    private suspend fun runSamplingPhase(
        phase: SessionPhase,
        currentSampleCount: () -> Int,
    ) {
        val startMs = System.currentTimeMillis()
        val minMs = MIN_PHASE_DURATION_SEC * 1000L
        val maxMs = MAX_PHASE_DURATION_SEC * 1000L
        Log.d(TAG, "Phase enter: $phase (min=${MIN_PHASE_DURATION_SEC}s, max=${MAX_PHASE_DURATION_SEC}s)")
        _state.update { it.copy(phase = phase, phaseRemainingSeconds = MAX_PHASE_DURATION_SEC) }

        while (true) {
            delay(250)    // tick lebih sering untuk responsiveness UI
            val elapsed = System.currentTimeMillis() - startMs
            val samples = currentSampleCount()
            val remainingSec = ((maxMs - elapsed) / 1000).toInt().coerceAtLeast(0)
            _state.update { it.copy(phaseRemainingSeconds = remainingSec) }

            // Stop conditions
            if (elapsed >= maxMs) {
                Log.d(TAG, "Phase $phase: max duration reached, samples=$samples")
                break
            }
            if (elapsed >= minMs && samples >= MIN_SAMPLES_PER_PHASE) {
                Log.d(TAG, "Phase $phase: min samples reached at ${elapsed / 1000}s ($samples samples)")
                break
            }
        }
        Log.d(TAG, "Phase exit: $phase. ext=${extensionLagSamples.size}, flex=${flexionSamples.size}")
    }

    private suspend fun finalize() {
        val flexCount = flexionSamples.size
        val extCount = extensionLagSamples.size
        Log.d(TAG, "Finalize: extensionSamples=$extCount, flexionSamples=$flexCount")

        _state.update { it.copy(phase = SessionPhase.SAVING) }

        // Validasi minimum sample. Threshold rendah (10) supaya jalan di
        // emulator / device dengan frame rate 2-5 fps (heavy model).
        if (flexCount < MIN_SAMPLES_PER_PHASE || extCount < MIN_SAMPLES_PER_PHASE) {
            _state.update {
                it.copy(
                    phase = SessionPhase.FAILED,
                    sessionErrorMessage = "Sample tidak cukup (extension: $extCount, flexion: $flexCount, " +
                        "min: $MIN_SAMPLES_PER_PHASE). Device Anda mungkin terlalu lambat untuk model " +
                        "pose Heavy. Tutup aplikasi lain (terutama screen recorder) lalu coba lagi. " +
                        "Kalau tetap gagal, hubungi developer untuk swap ke model Lite.",
                )
            }
            return
        }

        // Percentile finalize: P95 untuk flexion (max), P5 untuk extLag (min)
        val maxFlex = Percentile.compute(flexionSamples, 95f)
        val minLag = Percentile.compute(extensionLagSamples, 5f)

        if (maxFlex.isNaN() || minLag.isNaN()) {
            _state.update {
                it.copy(
                    phase = SessionPhase.FAILED,
                    sessionErrorMessage = "Hasil tidak valid. Silakan ulang pengukuran.",
                )
            }
            return
        }

        val category = RomClassifier.classify(maxFlex, minLag)

        val side = _state.value.selectedKneeSide ?: KneeSide.RIGHT
        val uid = authRepo.currentUid()
        if (uid == null) {
            _state.update {
                it.copy(
                    phase = SessionPhase.FAILED,
                    sessionErrorMessage = "Sesi login expired. Silakan login ulang.",
                )
            }
            return
        }

        val nowMs = System.currentTimeMillis()
        val result = RomResult(
            id = "rom_${nowMs}_${UUID.randomUUID().toString().take(8)}",
            userId = uid,
            timestampMs = nowMs,
            kneeSide = side,
            maxFlexionDeg = maxFlex,
            maxExtensionLagDeg = minLag,
            category = category,
            sessionDurationMs = nowMs - sessionStartedAtMs,
            deviceModel = Build.MODEL ?: "unknown",
            mediaPipeModel = MEDIAPIPE_MODEL_VERSION,
        )

        val saveResult = romRepo.upsert(result)
        if (saveResult.isSuccess) {
            _state.update {
                it.copy(
                    phase = SessionPhase.DONE,
                    completedRomId = result.id,
                    completedRom = result,
                )
            }
        } else {
            val err = saveResult.exceptionOrNull()
            Log.e(TAG, "Failed to save RomResult", err)
            _state.update {
                it.copy(
                    phase = SessionPhase.FAILED,
                    sessionErrorMessage = "Gagal menyimpan ke server: ${err?.message ?: "—"}. " +
                        "Hasil mungkin tersimpan offline dan akan sync saat online.",
                )
            }
        }
    }

    private fun processPose(detection: PoseDetection) {
        val side = _state.value.selectedKneeSide ?: return

        if (detection.isEmpty()) {
            _state.update {
                it.copy(poseQuality = PoseQuality.NotDetected, currentAngleDeg = null)
            }
            return
        }

        val (hipIdx, kneeIdx, ankleIdx) = when (side) {
            KneeSide.LEFT -> Triple(
                PoseDetection.LEFT_HIP, PoseDetection.LEFT_KNEE, PoseDetection.LEFT_ANKLE,
            )
            KneeSide.RIGHT -> Triple(
                PoseDetection.RIGHT_HIP, PoseDetection.RIGHT_KNEE, PoseDetection.RIGHT_ANKLE,
            )
            KneeSide.BOTH -> return
        }

        val hip = detection.landmarks.getOrNull(hipIdx) ?: return
        val knee = detection.landmarks.getOrNull(kneeIdx) ?: return
        val ankle = detection.landmarks.getOrNull(ankleIdx) ?: return

        val minVis = minOf(
            detection.visibilityOf(hipIdx),
            detection.visibilityOf(kneeIdx),
            detection.visibilityOf(ankleIdx),
        )
        val quality = when {
            minVis >= 0.7f -> PoseQuality.Good
            minVis >= 0.4f -> PoseQuality.Low
            else -> PoseQuality.NotDetected
        }

        if (quality == PoseQuality.NotDetected) {
            _state.update { it.copy(poseQuality = quality, currentAngleDeg = null) }
            return
        }

        val interior = RomAngleCalculator.computeKneeInteriorAngle(hip, knee, ankle)
        if (interior.isNaN() || interior < 30f || interior > 195f) {
            // Reject artefak — sudut tidak masuk akal anatomis
            _state.update { it.copy(poseQuality = quality) }
            return
        }
        val flexion = RomAngleCalculator.toFlexion(interior)

        _state.update {
            it.copy(
                poseQuality = quality,
                currentAngleDeg = interior,
                currentFlexionDeg = flexion,
            )
        }

        // Sample collection — saat fase aktif. Terima Good DAN Low quality
        // (vis ≥ 0.4) supaya tidak miss frame saat user gerak cepat.
        // NotDetected sudah di-filter di atas.
        val currentPhase = _state.value.phase
        when (currentPhase) {
            SessionPhase.EXTENSION -> {
                extensionLagSamples += flexion
                if (extensionLagSamples.size % 10 == 0) {
                    Log.d(TAG, "EXTENSION samples: ${extensionLagSamples.size} (last flex=$flexion)")
                }
            }
            SessionPhase.FLEXION -> {
                flexionSamples += flexion
                if (flexionSamples.size % 10 == 0) {
                    Log.d(TAG, "FLEXION samples: ${flexionSamples.size} (last flex=$flexion)")
                }
            }
            else -> { /* skip — di luar fase aktif */ }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionJob?.cancel()
        poseJob?.cancel()
    }

    companion object {
        private const val TAG = "ScanVM"

        /**
         * Minimum samples per fase untuk percentile finalize bermakna.
         * 8 samples = P95 ambil top 1, P5 ambil bottom 1 — masih ada robustness.
         * Bisa di-bump ke 10-15 kalau device target lebih cepat.
         */
        private const val MIN_SAMPLES_PER_PHASE = 8

        /**
         * Adaptive duration: device cepat selesai 5s, slow dapat tambahan
         * sampai 10s untuk capai sample minimum.
         */
        private const val MIN_PHASE_DURATION_SEC = 5
        private const val MAX_PHASE_DURATION_SEC = 10

        private const val MEDIAPIPE_MODEL_VERSION = "pose_landmarker_heavy_v0.10"
    }
}

data class ScanUiState(
    val selectedKneeSide: KneeSide? = null,
    val currentAngleDeg: Float? = null,
    val currentFlexionDeg: Float? = null,
    val poseQuality: PoseQuality = PoseQuality.NotDetected,

    val phase: SessionPhase = SessionPhase.IDLE,
    val phaseRemainingSeconds: Int = 0,
    val sessionErrorMessage: String? = null,
    val completedRomId: String? = null,
    val completedRom: RomResult? = null,
)

enum class PoseQuality { NotDetected, Low, Good }
