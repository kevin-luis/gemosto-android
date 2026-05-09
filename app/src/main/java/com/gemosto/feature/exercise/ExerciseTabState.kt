package com.gemosto.feature.exercise

/**
 * Step di flow Exercise tab.
 *
 * - Program: tampil overview program (default state)
 * - Session: tampil ExerciseDetailScreen sequential, dengan index latihan saat ini
 * - PostSession: post-session pain dialog (Hari 12 — placeholder return ke Program)
 */
sealed interface ExerciseTabState {
    data object Program : ExerciseTabState
    data class Session(val exerciseIndex: Int = 0) : ExerciseTabState
    data class PostSession(val stoppedDueToPain: Boolean) : ExerciseTabState
}
