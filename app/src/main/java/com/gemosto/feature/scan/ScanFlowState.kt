package com.gemosto.feature.scan

/**
 * Step di flow Scan ROM.
 *
 * Hari 5: hanya Intro + Camera (preview only).
 * Hari 6-7: Camera dengan MediaPipe pose + 2-phase measurement.
 * Hari 8: Result step.
 */
sealed interface ScanFlowState {
    data object Intro : ScanFlowState
    data object Camera : ScanFlowState
    // data class Result(val romId: String) : ScanFlowState  // Hari 8
}
