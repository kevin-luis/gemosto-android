package com.gemosto.domain.rom

import com.gemosto.domain.model.RomCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test untuk RomClassifier — boundary tiap kategori.
 *
 * Per spec 002 AC21: mencakup boundary 129/130, 110/109, 5/6, 10/11, 20/21.
 */
class RomClassifierTest {

    // ─── Single-axis: flexion only (extLag bagus) ──────────────────

    @Test
    fun `flexion 130 with extLag 5 returns NORMAL`() {
        assertEquals(RomCategory.NORMAL, RomClassifier.classify(130f, 5f))
    }

    @Test
    fun `flexion 150 with extLag 0 returns NORMAL`() {
        assertEquals(RomCategory.NORMAL, RomClassifier.classify(150f, 0f))
    }

    @Test
    fun `flexion 129 with extLag 0 returns MILD (boundary)`() {
        assertEquals(RomCategory.MILD, RomClassifier.classify(129f, 0f))
    }

    @Test
    fun `flexion 110 with extLag 0 returns MILD (boundary)`() {
        assertEquals(RomCategory.MILD, RomClassifier.classify(110f, 0f))
    }

    @Test
    fun `flexion 109 with extLag 0 returns MODERATE (boundary)`() {
        assertEquals(RomCategory.MODERATE, RomClassifier.classify(109f, 0f))
    }

    @Test
    fun `flexion 90 with extLag 0 returns MODERATE (boundary)`() {
        assertEquals(RomCategory.MODERATE, RomClassifier.classify(90f, 0f))
    }

    @Test
    fun `flexion 89 with extLag 0 returns SEVERE`() {
        assertEquals(RomCategory.SEVERE, RomClassifier.classify(89f, 0f))
    }

    @Test
    fun `flexion 50 with extLag 0 returns SEVERE`() {
        assertEquals(RomCategory.SEVERE, RomClassifier.classify(50f, 0f))
    }

    // ─── Single-axis: extLag only (flexion bagus) ──────────────────

    @Test
    fun `flexion 150 with extLag 5 returns NORMAL (boundary)`() {
        assertEquals(RomCategory.NORMAL, RomClassifier.classify(150f, 5f))
    }

    @Test
    fun `flexion 150 with extLag 6 returns MILD (boundary)`() {
        assertEquals(RomCategory.MILD, RomClassifier.classify(150f, 6f))
    }

    @Test
    fun `flexion 150 with extLag 10 returns MILD (boundary)`() {
        assertEquals(RomCategory.MILD, RomClassifier.classify(150f, 10f))
    }

    @Test
    fun `flexion 150 with extLag 11 returns MODERATE (boundary)`() {
        assertEquals(RomCategory.MODERATE, RomClassifier.classify(150f, 11f))
    }

    @Test
    fun `flexion 150 with extLag 20 returns MODERATE (boundary)`() {
        assertEquals(RomCategory.MODERATE, RomClassifier.classify(150f, 20f))
    }

    @Test
    fun `flexion 150 with extLag 21 returns SEVERE`() {
        assertEquals(RomCategory.SEVERE, RomClassifier.classify(150f, 21f))
    }

    // ─── Combined (worse-of dua dimensi) ───────────────────────────

    @Test
    fun `flexion NORMAL but extLag MILD returns MILD`() {
        assertEquals(RomCategory.MILD, RomClassifier.classify(140f, 8f))
    }

    @Test
    fun `flexion MILD but extLag MODERATE returns MODERATE`() {
        assertEquals(RomCategory.MODERATE, RomClassifier.classify(120f, 15f))
    }

    @Test
    fun `flexion MODERATE but extLag SEVERE returns SEVERE`() {
        assertEquals(RomCategory.SEVERE, RomClassifier.classify(100f, 25f))
    }

    @Test
    fun `flexion SEVERE with extLag NORMAL returns SEVERE`() {
        assertEquals(RomCategory.SEVERE, RomClassifier.classify(70f, 3f))
    }
}
