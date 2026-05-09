package com.gemosto.domain.model

import com.gemosto.domain.exercise.Exercise

/**
 * Level latihan yang dihasilkan ExerciseRuleEngine.
 *
 * - BLOCK: tidak ada latihan, dorong konsultasi dokter
 * - GENTLE: isometric & low-impact
 * - STRENGTHENING: penguatan terkontrol
 * - FUNCTIONAL: gerakan fungsional (squat, step-up, dll)
 *
 * Detail: spec 003 section 6.2.
 */
enum class ExerciseLevel(val displayLabel: String) {
    BLOCK("Konsultasi Dokter"),
    GENTLE("Gerakan Lembut"),
    STRENGTHENING("Penguatan"),
    FUNCTIONAL("Fungsional"),
}

/**
 * Status program latihan.
 */
enum class ProgramStatus {
    ACTIVE,
    ARCHIVED,
    BLOCKED_PAIN,
}

/**
 * Sumber narasi program — apakah dari Gemini atau fallback statis.
 */
enum class NarrativeSource {
    GEMINI,
    FALLBACK_STATIC,
}

/**
 * Narasi exercise program — output dari Gemini AI.
 *
 * **Kontrak penting:** Gemini hanya isi 3 field text ini.
 * Parameter klinis (gerakan, sets, reps) di-drive sepenuhnya oleh
 * [com.gemosto.domain.exercise.ExerciseRuleEngine] yang deterministik.
 *
 * Spec: 003-exercise-recommendation.md section 7.1.
 */
data class ExerciseNarrative(
    val intro: String,             // max 2 kalimat sapaan personal
    val rationale: String,         // max 3 kalimat alasan medis sederhana
    val weeklyMotivation: String,  // 1 kalimat motivasi mingguan
    val source: NarrativeSource,
)

/**
 * Program latihan rumahan yang di-generate untuk user.
 *
 * Disimpan di Firestore path `users/{uid}/programs/{programId}`.
 *
 * Field `exercises` adalah list latihan dengan parameter klinis sudah
 * disesuaikan oleh [com.gemosto.domain.exercise.ExerciseRuleEngine.adjustDose].
 *
 * Field `narrative` null saat awal generate (Gemini call paralel) —
 * akan di-update setelah Gemini selesai atau fallback statis.
 *
 * Spec: 003-exercise-recommendation.md section 5.
 */
data class ExerciseProgram(
    val id: String,
    val userId: String,
    val generatedAt: Long,
    val basedOnRomId: String,
    val romCategory: RomCategory,
    val level: ExerciseLevel,
    val durationWeeks: Int,
    val frequencyPerWeek: String,
    val exercises: List<Exercise>,         // full list dengan dosis adjusted
    val narrative: ExerciseNarrative?,     // null sampai Gemini call selesai
    val status: ProgramStatus,
    val safetyNote: String? = null,
) {
    /** Convenience untuk Card Home (sebelumnya field standalone). */
    val exerciseCount: Int get() = exercises.size
}
