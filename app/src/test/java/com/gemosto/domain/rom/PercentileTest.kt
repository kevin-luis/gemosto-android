package com.gemosto.domain.rom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PercentileTest {

    private val tol = 0.001f

    @Test
    fun `empty list returns NaN`() {
        assertTrue(Percentile.compute(emptyList(), 95f).isNaN())
    }

    @Test
    fun `single element returns that element`() {
        assertEquals(7.5f, Percentile.compute(listOf(7.5f), 50f), tol)
        assertEquals(7.5f, Percentile.compute(listOf(7.5f), 95f), tol)
    }

    @Test
    fun `P50 of 1 to 5 returns median 3`() {
        assertEquals(3f, Percentile.compute(listOf(1f, 2f, 3f, 4f, 5f), 50f), tol)
    }

    @Test
    fun `P0 returns minimum`() {
        assertEquals(1f, Percentile.compute(listOf(5f, 1f, 3f, 2f, 4f), 0f), tol)
    }

    @Test
    fun `P100 returns maximum`() {
        assertEquals(5f, Percentile.compute(listOf(5f, 1f, 3f, 2f, 4f), 100f), tol)
    }

    @Test
    fun `P95 robustness against single outlier high`() {
        // 19 nilai 100 + 1 nilai 999 — P95 harusnya tetap dekat 100
        val values = List(19) { 100f } + listOf(999f)
        val result = Percentile.compute(values, 95f)
        assertTrue("Expected near 100, got $result", result < 200f)
    }

    @Test
    fun `P5 robustness against single outlier low`() {
        // 19 nilai 100 + 1 nilai -999 — P5 harusnya tetap dekat 100
        val values = List(19) { 100f } + listOf(-999f)
        val result = Percentile.compute(values, 5f)
        assertTrue("Expected near 100, got $result", result > 0f)
    }

    @Test
    fun `P95 of typical flexion samples`() {
        // Simulate 100 frame dengan flexion noise di sekitar 130°
        val samples = (0 until 100).map { 130f + (it % 5) - 2 }   // 128..132
        val p95 = Percentile.compute(samples, 95f)
        assertTrue("Expected 131..133, got $p95", p95 in 131f..133f)
    }

    @Test
    fun `clamp p over 100`() {
        // Tidak crash, treated as 100
        val r = Percentile.compute(listOf(1f, 2f, 3f), 150f)
        assertEquals(3f, r, tol)
    }

    @Test
    fun `clamp negative p`() {
        val r = Percentile.compute(listOf(1f, 2f, 3f), -10f)
        assertEquals(1f, r, tol)
    }

    @Test
    fun `does not mutate input list`() {
        val input = listOf(5f, 1f, 3f)
        Percentile.compute(input, 50f)
        // Input order tetap
        assertEquals(listOf(5f, 1f, 3f), input)
    }
}
