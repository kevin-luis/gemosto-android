package com.gemosto.domain.model

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
 * Placeholder model — full schema akan diisi di Hari 9-11 (spec 003).
 *
 * Untuk Hari 4 (Home Dashboard), HomeViewModel hanya butuh field minimum
 * untuk render Card "Program Saya".
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
    val exerciseCount: Int,
    val status: ProgramStatus,
    val safetyNote: String? = null,
)
