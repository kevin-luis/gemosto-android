package com.gemosto.domain.exercise

import com.gemosto.domain.exercise.ExerciseRuleEngine.Companion.SAFETY_NOTE_PAIN_BLOCK
import com.gemosto.domain.exercise.ExerciseRuleEngine.Companion.SAFETY_NOTE_RECENT_HIGH_PAIN
import com.gemosto.domain.exercise.ExerciseRuleEngine.Companion.SAFETY_NOTE_SEVERE_BLOCK
import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.RomCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests untuk [ExerciseRuleEngine].
 *
 * Coverage target — tiap branch decision tree (spec 003 section 6.3):
 *   - 5 ROM × Age × Activity matrix → level mapping
 *   - 5 Safety block (SEVERE, pain ≥8 berturut)
 *   - 6 Dose adjustment (age cutoff × pain cutoff × max -1 cap)
 *   - 3 Recent-pain safety note
 *   - 2 Frequency & duration metadata
 *   - 1 Determinism (input sama → output identik)
 *   - Total ≥ 22 test (lebih dari syarat AC11 = 18)
 */
class ExerciseRuleEngineTest {

    private lateinit var engine: ExerciseRuleEngine

    @Before
    fun setUp() {
        engine = ExerciseRuleEngine()
    }

    // ────────────────────────────────────────────────────────────────────
    // ROM × Age × Activity matrix → ExerciseLevel
    // AC2, AC4, AC5, AC6, AC7

    @Test
    fun `severe ROM returns BLOCK with empty exercises and severe safety note`() {
        val out = engine.compute(
            input(rom = RomCategory.SEVERE, age = 35, activity = ActivityLevel.ACTIVE),
        )

        assertEquals(ExerciseLevel.BLOCK, out.level)
        assertTrue("Exercises should be empty for BLOCK", out.exercises.isEmpty())
        assertEquals(0, out.durationWeeks)
        assertEquals("—", out.frequencyPerWeek)
        assertEquals(SAFETY_NOTE_SEVERE_BLOCK, out.safetyNote)
    }

    @Test
    fun `moderate ROM returns GENTLE with 4 exercises (quad set, heel slide, ankle pump, glute squeeze)`() {
        val out = engine.compute(
            input(rom = RomCategory.MODERATE, age = 35, activity = ActivityLevel.ACTIVE),
        )

        assertEquals(ExerciseLevel.GENTLE, out.level)
        assertEquals(4, out.exercises.size)
        val ids = out.exercises.map { it.id }
        assertEquals(
            listOf(
                ExerciseId.QUAD_SETS,
                ExerciseId.HEEL_SLIDES,
                ExerciseId.ANKLE_PUMPS,
                ExerciseId.GLUTE_SQUEEZE,
            ),
            ids,
        )
    }

    @Test
    fun `mild ROM returns STRENGTHENING with 5 exercises regardless of activity`() {
        val out = engine.compute(
            input(rom = RomCategory.MILD, age = 40, activity = ActivityLevel.LOW),
        )

        assertEquals(ExerciseLevel.STRENGTHENING, out.level)
        assertEquals(5, out.exercises.size)
        assertTrue(out.exercises.any { it.id == ExerciseId.STRAIGHT_LEG_RAISE })
        assertTrue(out.exercises.any { it.id == ExerciseId.WALL_SIT_SHORT })
    }

    @Test
    fun `normal ROM with age under 50 and ACTIVE level returns FUNCTIONAL`() {
        val out = engine.compute(
            input(rom = RomCategory.NORMAL, age = 35, activity = ActivityLevel.ACTIVE),
        )

        assertEquals(ExerciseLevel.FUNCTIONAL, out.level)
        assertEquals(5, out.exercises.size)
        val ids = out.exercises.map { it.id }
        assertEquals(
            listOf(
                ExerciseId.WALL_SQUAT,
                ExerciseId.STEP_UPS_LOW,
                ExerciseId.BRIDGES,
                ExerciseId.CALF_RAISES,
                ExerciseId.STRAIGHT_LEG_RAISE,
            ),
            ids,
        )
    }

    @Test
    fun `normal ROM with age 50 and ACTIVE returns STRENGTHENING (age cutoff inclusive)`() {
        // age 50 = bukan FUNCTIONAL (cutoff < 50)
        val out = engine.compute(
            input(rom = RomCategory.NORMAL, age = 50, activity = ActivityLevel.ACTIVE),
        )

        assertEquals(ExerciseLevel.STRENGTHENING, out.level)
    }

    @Test
    fun `normal ROM with young user but MODERATE activity returns STRENGTHENING`() {
        val out = engine.compute(
            input(rom = RomCategory.NORMAL, age = 30, activity = ActivityLevel.MODERATE),
        )

        assertEquals(ExerciseLevel.STRENGTHENING, out.level)
    }

    @Test
    fun `normal ROM with young user but LOW activity returns STRENGTHENING`() {
        val out = engine.compute(
            input(rom = RomCategory.NORMAL, age = 30, activity = ActivityLevel.LOW),
        )

        assertEquals(ExerciseLevel.STRENGTHENING, out.level)
    }

    // ────────────────────────────────────────────────────────────────────
    // Safety block — pain ≥8 berturut atau SEVERE
    // AC3

    @Test
    fun `consecutive high pain count of 2 returns BLOCK even for NORMAL ROM`() {
        // Override: walaupun ROM NORMAL + ACTIVE + young (yang seharusnya FUNCTIONAL),
        // 2x pain berturut tinggi tetap memblok program.
        val out = engine.compute(
            input(
                rom = RomCategory.NORMAL,
                age = 30,
                activity = ActivityLevel.ACTIVE,
                consecutiveHighPainCount = 2,
            ),
        )

        assertEquals(ExerciseLevel.BLOCK, out.level)
        assertTrue(out.exercises.isEmpty())
        assertEquals(SAFETY_NOTE_PAIN_BLOCK, out.safetyNote)
    }

    @Test
    fun `consecutive high pain count of 3 also returns BLOCK`() {
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 40,
                activity = ActivityLevel.MODERATE,
                consecutiveHighPainCount = 3,
            ),
        )

        assertEquals(ExerciseLevel.BLOCK, out.level)
        assertEquals(SAFETY_NOTE_PAIN_BLOCK, out.safetyNote)
    }

    @Test
    fun `consecutive high pain count of 1 does not block — returns normal program`() {
        // Single high-pain entry tidak cukup untuk block.
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 40,
                activity = ActivityLevel.MODERATE,
                consecutiveHighPainCount = 1,
            ),
        )

        assertEquals(ExerciseLevel.STRENGTHENING, out.level)
        assertTrue(out.exercises.isNotEmpty())
    }

    @Test
    fun `severe ROM combined with consecutive high pain — pain block message takes priority`() {
        // STEP 1 (pain block) di-evaluasi lebih dulu dari STEP 2 (level)
        val out = engine.compute(
            input(
                rom = RomCategory.SEVERE,
                age = 35,
                activity = ActivityLevel.LOW,
                consecutiveHighPainCount = 2,
            ),
        )

        assertEquals(ExerciseLevel.BLOCK, out.level)
        assertEquals(SAFETY_NOTE_PAIN_BLOCK, out.safetyNote)
    }

    // ────────────────────────────────────────────────────────────────────
    // Dose adjustment — age cutoff × pain cutoff × max -1 cap
    // AC8, AC9, AC10

    @Test
    fun `age 40 with no pain keeps catalog default sets`() {
        val out = engine.compute(
            input(rom = RomCategory.MILD, age = 40, activity = ActivityLevel.MODERATE),
        )

        // SLR catalog default: 3 set
        val slr = out.exercises.first { it.id == ExerciseId.STRAIGHT_LEG_RAISE }
        assertEquals(3, slr.sets)
    }

    @Test
    fun `age 50 with no pain reduces 1 set per exercise`() {
        val out = engine.compute(
            input(rom = RomCategory.MILD, age = 50, activity = ActivityLevel.MODERATE),
        )

        val slr = out.exercises.first { it.id == ExerciseId.STRAIGHT_LEG_RAISE }
        assertEquals(2, slr.sets) // 3 → 2
    }

    @Test
    fun `age 60 with no pain reduces 1 set per exercise`() {
        val out = engine.compute(
            input(rom = RomCategory.MILD, age = 60, activity = ActivityLevel.LOW),
        )

        val slr = out.exercises.first { it.id == ExerciseId.STRAIGHT_LEG_RAISE }
        assertEquals(2, slr.sets) // 3 → 2
    }

    @Test
    fun `recent pain score 5 reduces 1 set per exercise (boundary inclusive)`() {
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 40,
                activity = ActivityLevel.MODERATE,
                recentPainScore = 5,
            ),
        )

        val slr = out.exercises.first { it.id == ExerciseId.STRAIGHT_LEG_RAISE }
        assertEquals(2, slr.sets) // 3 → 2
    }

    @Test
    fun `recent pain score 4 keeps catalog default sets (below threshold)`() {
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 40,
                activity = ActivityLevel.MODERATE,
                recentPainScore = 4,
            ),
        )

        val slr = out.exercises.first { it.id == ExerciseId.STRAIGHT_LEG_RAISE }
        assertEquals(3, slr.sets)
    }

    @Test
    fun `age 50 plus pain 6 reduces only one set total — not two (max -1 cap)`() {
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 50,
                activity = ActivityLevel.MODERATE,
                recentPainScore = 6,
            ),
        )

        val slr = out.exercises.first { it.id == ExerciseId.STRAIGHT_LEG_RAISE }
        // Bukan 1 (3 - 2), bukan 3 (no reduction). Harus 2 — capped at -1.
        assertEquals(2, slr.sets)
    }

    @Test
    fun `dose reduction never goes below 1 set per exercise`() {
        // Quad Sets default = 2 set; setelah -1 = 1 (boundary).
        val out = engine.compute(
            input(
                rom = RomCategory.MODERATE,
                age = 60,
                activity = ActivityLevel.LOW,
                recentPainScore = 8,
            ),
        )

        out.exercises.forEach {
            assertTrue(
                "All exercises must have ≥1 set after reduction (got ${it.sets} for ${it.id})",
                it.sets >= 1,
            )
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Recent-pain safety note (≥7 → muncul, <7 → null)

    @Test
    fun `recent pain 7 adds safety note about reducing intensity`() {
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 40,
                activity = ActivityLevel.MODERATE,
                recentPainScore = 7,
            ),
        )

        assertNotNull(out.safetyNote)
        assertEquals(SAFETY_NOTE_RECENT_HIGH_PAIN, out.safetyNote)
        // Dan tetap return program normal (belum block)
        assertEquals(ExerciseLevel.STRENGTHENING, out.level)
    }

    @Test
    fun `recent pain 6 does not add safety note (below threshold)`() {
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 40,
                activity = ActivityLevel.MODERATE,
                recentPainScore = 6,
            ),
        )

        assertNull(out.safetyNote)
    }

    @Test
    fun `null recent pain score does not add safety note`() {
        val out = engine.compute(
            input(
                rom = RomCategory.MILD,
                age = 40,
                activity = ActivityLevel.MODERATE,
                recentPainScore = null,
            ),
        )

        assertNull(out.safetyNote)
    }

    // ────────────────────────────────────────────────────────────────────
    // Frequency & duration metadata

    @Test
    fun `duration is 4 weeks for non-block programs and frequency matches level`() {
        val gentle = engine.compute(
            input(rom = RomCategory.MODERATE, age = 35, activity = ActivityLevel.MODERATE),
        )
        val strengthening = engine.compute(
            input(rom = RomCategory.MILD, age = 35, activity = ActivityLevel.MODERATE),
        )
        val functional = engine.compute(
            input(rom = RomCategory.NORMAL, age = 35, activity = ActivityLevel.ACTIVE),
        )

        assertEquals(4, gentle.durationWeeks)
        assertEquals(4, strengthening.durationWeeks)
        assertEquals(4, functional.durationWeeks)

        assertEquals("5x/minggu", gentle.frequencyPerWeek)
        assertEquals("3-4x/minggu", strengthening.frequencyPerWeek)
        assertEquals("3x/minggu", functional.frequencyPerWeek)
    }

    @Test
    fun `block output has zero duration and dash frequency`() {
        val out = engine.compute(
            input(rom = RomCategory.SEVERE, age = 35, activity = ActivityLevel.MODERATE),
        )

        assertEquals(0, out.durationWeeks)
        assertEquals("—", out.frequencyPerWeek)
    }

    // ────────────────────────────────────────────────────────────────────
    // AC1 — Determinism

    @Test
    fun `compute is deterministic — same input produces identical output across many calls`() {
        val same = input(
            rom = RomCategory.MILD,
            age = 50,
            activity = ActivityLevel.MODERATE,
            recentPainScore = 7,
            consecutiveHighPainCount = 1,
        )

        val first = engine.compute(same)
        repeat(100) {
            val out = engine.compute(same)
            assertEquals("Output must equal first run on iteration $it", first, out)
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Helper

    private fun input(
        rom: RomCategory,
        age: Int,
        activity: ActivityLevel,
        recentPainScore: Int? = null,
        consecutiveHighPainCount: Int = 0,
    ) = ExerciseRuleEngine.Input(
        romCategory = rom,
        age = age,
        activityLevel = activity,
        recentPainScore = recentPainScore,
        consecutiveHighPainCount = consecutiveHighPainCount,
    )
}
