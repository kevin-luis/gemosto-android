package com.gemosto.domain.exercise

import com.gemosto.domain.model.ExerciseLevel

/**
 * Definisi satu latihan dalam program rumahan.
 *
 * Field sets/reps/restSeconds adalah PARAMETER KLINIS yang ditentukan
 * deterministik oleh [ExerciseRuleEngine] (boleh dimodifikasi via
 * adjustDose) — Gemini AI **tidak boleh** mengubah field ini.
 *
 * Field name/description/tips/warnings di-bundle statis di [ExerciseCatalog]
 * dan dipakai langsung di UI ExerciseDetailScreen.
 *
 * Spec: 003-exercise-recommendation.md section 5.
 */
data class Exercise(
    val id: ExerciseId,
    val name: String,
    val level: ExerciseLevel,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val estimatedMinutes: Int,
    val description: List<String>,   // 3-5 bullet "Cara Melakukan"
    val tips: List<String>,          // 2-3 bullet tips
    val warnings: List<String>,      // 2-3 bullet "Hentikan jika..."
)
