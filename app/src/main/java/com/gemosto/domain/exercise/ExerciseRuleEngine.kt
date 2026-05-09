package com.gemosto.domain.exercise

import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.RomCategory

/**
 * Rule engine deterministik untuk menentukan program latihan rumahan
 * berdasar kategori ROM, profil user, dan riwayat pain log.
 *
 * Engine ini adalah PURE Kotlin — tidak boleh import Android, Firebase,
 * atau library platform lainnya. Wajib di-unit-test menyeluruh.
 *
 * **Argumen safety skripsi:**
 * Parameter klinis (gerakan, sets, reps) ditentukan oleh aturan eksplisit
 * yang dapat diverifikasi referensi medis-nya. AI generatif (Gemini) TIDAK
 * dilibatkan dalam keputusan klinis — Gemini hanya generate narasi
 * (intro, rationale, motivasi).
 *
 * Decision tree (lihat juga tabel di spec 003 section 6.3):
 *
 *   STEP 1 (Safety Block):
 *     consecutiveHighPain ≥ 2  → BLOCK + safetyNote
 *
 *   STEP 2 (Level via ROM × Age × Activity):
 *     SEVERE                                      → BLOCK
 *     MODERATE                                    → GENTLE
 *     MILD                                        → STRENGTHENING
 *     NORMAL + age < 50 + ACTIVE                  → FUNCTIONAL
 *     NORMAL + (age ≥ 50 OR activity != ACTIVE)   → STRENGTHENING
 *
 *   STEP 3 (Pick Exercises): subset dari [ExerciseCatalog]
 *
 *   STEP 4 (Adjust Dose):
 *     age ≥ 50              → -1 set
 *     recentPainScore ≥ 5   → -1 set
 *     reduksi maks total    → -1 set (tidak akumulatif -2)
 *     hasil minimal         → 1 set per latihan
 *
 * Spec: 003-exercise-recommendation.md section 6.2.
 */
class ExerciseRuleEngine {

    /**
     * Input untuk [compute]. Semua field nilai-nya adalah snapshot pada
     * saat program di-generate — engine tidak query state apapun.
     *
     * @param recentPainScore         pain log terakhir (0..10), atau null
     *                                kalau user belum pernah input pain log.
     * @param consecutiveHighPainCount jumlah pain log berturut dengan
     *                                 score ≥ 8 (paling baru → mundur).
     */
    data class Input(
        val romCategory: RomCategory,
        val age: Int,
        val activityLevel: ActivityLevel,
        val recentPainScore: Int? = null,
        val consecutiveHighPainCount: Int = 0,
    )

    /**
     * Output [compute]. Saat `level == BLOCK`, list `exercises` selalu
     * empty dan `safetyNote` selalu non-null.
     */
    data class Output(
        val level: ExerciseLevel,
        val exercises: List<Exercise>,
        val durationWeeks: Int,
        val frequencyPerWeek: String,
        val safetyNote: String?,
    )

    /**
     * Compute program latihan deterministik. Input sama → output identik.
     */
    fun compute(input: Input): Output {
        // STEP 1 — Safety guard: pain berturut tinggi → BLOCK
        if (input.consecutiveHighPainCount >= PAIN_BLOCK_CONSECUTIVE_THRESHOLD) {
            return blockOutput(SAFETY_NOTE_PAIN_BLOCK)
        }

        // STEP 2 — Tentukan level
        val level = determineLevel(input)
        if (level == ExerciseLevel.BLOCK) {
            return blockOutput(SAFETY_NOTE_SEVERE_BLOCK)
        }

        // STEP 3 — Pilih exercise dari catalog
        val exercises = pickExercises(level).map(ExerciseCatalog::get)

        // STEP 4 — Adjust dosis
        val adjusted = exercises.map { adjustDose(it, input) }

        return Output(
            level = level,
            exercises = adjusted,
            durationWeeks = DEFAULT_DURATION_WEEKS,
            frequencyPerWeek = frequencyFor(level),
            safetyNote = safetyNoteFor(input.recentPainScore),
        )
    }

    // ────────────────────────────────────────────────────────────────────
    // Private helpers — semua deterministik & terisolasi untuk testability

    private fun blockOutput(note: String): Output = Output(
        level = ExerciseLevel.BLOCK,
        exercises = emptyList(),
        durationWeeks = 0,
        frequencyPerWeek = "—",
        safetyNote = note,
    )

    /**
     * Decision tree level. Tidak melibatkan pain — pain di-handle di Step 1
     * (block) dan Step 4 (dosis).
     */
    private fun determineLevel(input: Input): ExerciseLevel = when (input.romCategory) {
        RomCategory.SEVERE -> ExerciseLevel.BLOCK

        RomCategory.MODERATE -> ExerciseLevel.GENTLE

        RomCategory.MILD -> ExerciseLevel.STRENGTHENING

        RomCategory.NORMAL ->
            if (input.activityLevel == ActivityLevel.ACTIVE && input.age < AGE_FUNCTIONAL_CUTOFF) {
                ExerciseLevel.FUNCTIONAL
            } else {
                ExerciseLevel.STRENGTHENING
            }
    }

    /**
     * Pilih daftar [ExerciseId] untuk tiap level. Order yang dikembalikan
     * = order presentasi UI di ProgramScreen.
     */
    private fun pickExercises(level: ExerciseLevel): List<ExerciseId> = when (level) {
        ExerciseLevel.GENTLE -> listOf(
            ExerciseId.QUAD_SETS,
            ExerciseId.HEEL_SLIDES,
            ExerciseId.ANKLE_PUMPS,
            ExerciseId.GLUTE_SQUEEZE,
        )
        ExerciseLevel.STRENGTHENING -> listOf(
            ExerciseId.QUAD_SETS,
            ExerciseId.STRAIGHT_LEG_RAISE,
            ExerciseId.SHORT_ARC_QUAD,
            ExerciseId.HAMSTRING_CURL_HOLD,
            ExerciseId.WALL_SIT_SHORT,
        )
        ExerciseLevel.FUNCTIONAL -> listOf(
            ExerciseId.WALL_SQUAT,
            ExerciseId.STEP_UPS_LOW,
            ExerciseId.BRIDGES,
            ExerciseId.CALF_RAISES,
            ExerciseId.STRAIGHT_LEG_RAISE,
        )
        ExerciseLevel.BLOCK -> emptyList()
    }

    /**
     * Adjust set count berdasar age & pain. Reduksi total maks -1 set
     * (tidak akumulatif -2 walau dua kondisi terpenuhi).
     * Hasil minimum 1 set per latihan.
     */
    private fun adjustDose(exercise: Exercise, input: Input): Exercise {
        val ageReduction = if (input.age >= AGE_DOSE_REDUCTION_CUTOFF) 1 else 0
        val painReduction = if ((input.recentPainScore ?: 0) >= PAIN_DOSE_REDUCTION_THRESHOLD) 1 else 0
        val totalReduction = (ageReduction + painReduction).coerceAtMost(MAX_TOTAL_SET_REDUCTION)

        if (totalReduction == 0) return exercise

        return exercise.copy(
            sets = (exercise.sets - totalReduction).coerceAtLeast(MIN_SETS_AFTER_REDUCTION),
        )
    }

    private fun frequencyFor(level: ExerciseLevel): String = when (level) {
        ExerciseLevel.GENTLE -> "5x/minggu"
        ExerciseLevel.STRENGTHENING -> "3-4x/minggu"
        ExerciseLevel.FUNCTIONAL -> "3x/minggu"
        ExerciseLevel.BLOCK -> "—"
    }

    private fun safetyNoteFor(recentPainScore: Int?): String? =
        if (recentPainScore != null && recentPainScore >= PAIN_SAFETY_NOTE_THRESHOLD) {
            SAFETY_NOTE_RECENT_HIGH_PAIN
        } else {
            null
        }

    // ────────────────────────────────────────────────────────────────────
    // Konstanta — di-expose internal supaya bisa direference di test

    companion object {
        /** Minimum jumlah pain log berturut ≥8 supaya engine mem-block program. */
        const val PAIN_BLOCK_CONSECUTIVE_THRESHOLD = 2

        /** Threshold pain pada sesi terakhir → pemicu pengurangan 1 set. */
        const val PAIN_DOSE_REDUCTION_THRESHOLD = 5

        /** Threshold pain pada sesi terakhir → pemicu safety note di output. */
        const val PAIN_SAFETY_NOTE_THRESHOLD = 7

        /** Umur ≥ 50 → pemicu pengurangan 1 set. */
        const val AGE_DOSE_REDUCTION_CUTOFF = 50

        /** Umur < 50 (kombinasi dengan ACTIVE) syarat eligibility FUNCTIONAL. */
        const val AGE_FUNCTIONAL_CUTOFF = 50

        /** Maksimum reduksi set total (tidak akumulatif). */
        const val MAX_TOTAL_SET_REDUCTION = 1

        /** Set minimum setelah reduksi. */
        const val MIN_SETS_AFTER_REDUCTION = 1

        /** Durasi default program. */
        const val DEFAULT_DURATION_WEEKS = 4

        const val SAFETY_NOTE_PAIN_BLOCK =
            "Nyeri tinggi terdeteksi 2 kali berturut. " +
                "Konsultasikan dengan dokter sebelum melanjutkan latihan."

        const val SAFETY_NOTE_SEVERE_BLOCK =
            "Berdasarkan ROM Anda yang sangat terbatas, " +
                "kami sarankan konsultasi dokter sebelum mulai latihan rumahan."

        const val SAFETY_NOTE_RECENT_HIGH_PAIN =
            "Nyeri Anda di sesi sebelumnya cukup tinggi. " +
                "Lakukan latihan dengan intensitas lebih rendah."
    }
}
