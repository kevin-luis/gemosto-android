package com.gemosto.feature.scan

/**
 * Step di flow Scan ROM (level tab).
 *
 * Hari 8: Intro → Camera → Result(romId).
 */
sealed interface ScanFlowState {
    data object Intro : ScanFlowState
    data object Camera : ScanFlowState
    data class Result(val romId: String) : ScanFlowState
}
