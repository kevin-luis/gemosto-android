package com.gemosto.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.pose.PoseDetection
import com.gemosto.data.pose.PoseDetector
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.rom.RomAngleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State holder untuk flow Scan ROM.
 *
 * Hari 5: `selectedKneeSide` (init dari profile).
 * Hari 6: tambah `currentAngle` (live dari pose stream) + visibility.
 * Hari 7-8: 2-phase session state machine + sample collection + finalize.
 */
class ScanViewModel(
    private val poseDetector: PoseDetector,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    /**
     * Set default knee side berdasar profile.affectedKnee.
     * Kalau BOTH → default RIGHT (sesuai spec 002).
     */
    fun initFromProfile(affected: KneeSide) {
        if (_state.value.selectedKneeSide != null) return
        _state.value = _state.value.copy(
            selectedKneeSide = when (affected) {
                KneeSide.LEFT -> KneeSide.LEFT
                KneeSide.RIGHT -> KneeSide.RIGHT
                KneeSide.BOTH -> KneeSide.RIGHT
            },
        )
    }

    fun onKneeSelected(side: KneeSide) {
        if (side == KneeSide.BOTH) return
        _state.update { it.copy(selectedKneeSide = side) }
    }

    /**
     * Mulai mengkonsumsi pose detection stream — dipanggil saat enter Camera.
     * Konsumsi otomatis berhenti saat ViewModel cleared (viewModelScope cancelled).
     */
    fun startObservingPose() {
        viewModelScope.launch {
            poseDetector.detections.collect { detection ->
                processPose(detection)
            }
        }
    }

    private fun processPose(detection: PoseDetection) {
        val side = _state.value.selectedKneeSide ?: return

        if (detection.isEmpty()) {
            _state.update {
                it.copy(
                    poseQuality = PoseQuality.NotDetected,
                    currentAngleDeg = null,
                )
            }
            return
        }

        val (hipIdx, kneeIdx, ankleIdx) = when (side) {
            KneeSide.LEFT -> Triple(
                PoseDetection.LEFT_HIP,
                PoseDetection.LEFT_KNEE,
                PoseDetection.LEFT_ANKLE,
            )
            KneeSide.RIGHT -> Triple(
                PoseDetection.RIGHT_HIP,
                PoseDetection.RIGHT_KNEE,
                PoseDetection.RIGHT_ANKLE,
            )
            KneeSide.BOTH -> return
        }

        val hip = detection.landmarks.getOrNull(hipIdx) ?: return
        val knee = detection.landmarks.getOrNull(kneeIdx) ?: return
        val ankle = detection.landmarks.getOrNull(ankleIdx) ?: return

        val kneeVis = detection.visibilityOf(kneeIdx)
        val hipVis = detection.visibilityOf(hipIdx)
        val ankleVis = detection.visibilityOf(ankleIdx)
        val minVis = minOf(kneeVis, hipVis, ankleVis)

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
        val flexion = RomAngleCalculator.toFlexion(interior)

        _state.update {
            it.copy(
                poseQuality = quality,
                currentAngleDeg = if (interior.isNaN()) null else interior,
                currentFlexionDeg = if (flexion.isNaN()) null else flexion,
            )
        }
    }
}

data class ScanUiState(
    val selectedKneeSide: KneeSide? = null,
    /** Sudut interior lutut (180 = lurus, 0 = max flex). Null = belum ada / NaN. */
    val currentAngleDeg: Float? = null,
    /** Flexion = 180 - interior. Null = belum ada. */
    val currentFlexionDeg: Float? = null,
    val poseQuality: PoseQuality = PoseQuality.NotDetected,
)

enum class PoseQuality { NotDetected, Low, Good }
