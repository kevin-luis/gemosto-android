package com.gemosto.data.pose

import com.gemosto.domain.rom.Point2D

/**
 * Hasil deteksi pose untuk 1 frame.
 *
 * `landmarks` indeks mengikuti MediaPipe Pose Landmarker convention (33 landmark).
 * Yang relevan untuk lutut:
 *   23 = left_hip, 24 = right_hip
 *   25 = left_knee, 26 = right_knee
 *   27 = left_ankle, 28 = right_ankle
 *
 * `visibility[i]` 0..1 — confidence landmark i terlihat.
 *
 * Empty list → no pose detected.
 */
data class PoseDetection(
    val landmarks: List<Point2D>,
    val visibility: List<Float>,
    val timestampMs: Long,
) {
    companion object {
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_KNEE = 25
        const val RIGHT_KNEE = 26
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28

        val EMPTY = PoseDetection(emptyList(), emptyList(), 0L)
    }

    fun isEmpty(): Boolean = landmarks.isEmpty()

    fun visibilityOf(idx: Int): Float = visibility.getOrNull(idx) ?: 0f
}
