package com.gemosto.domain.rom

import kotlin.math.ceil

/**
 * Pure-Kotlin percentile calculation untuk robustness sample finalize.
 *
 * Pakai linear interpolation antar 2 closest rank — algoritma standar untuk
 * NIST percentile (sama dengan numpy default `linear` interpolation).
 *
 * Spec: 002-rom-scan.md section 6.3 — pakai P95 untuk max flexion (robust
 * terhadap 1-2 frame outlier high) dan P5 untuk min extension lag (robust
 * terhadap outlier low).
 */
object Percentile {

    /**
     * @param values list nilai (akan di-sort secara internal — input tidak dimutasi)
     * @param p percentile dalam range 0..100 (mis. 95f untuk P95)
     * @return nilai pada percentile `p`, atau Float.NaN kalau values kosong
     */
    fun compute(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return Float.NaN
        if (values.size == 1) return values[0]

        val pClamped = p.coerceIn(0f, 100f)
        val sorted = values.sorted()
        val rank = (pClamped / 100f) * (sorted.size - 1)
        val lowerIdx = rank.toInt()
        val upperIdx = ceil(rank.toDouble()).toInt().coerceAtMost(sorted.size - 1)
        val frac = rank - lowerIdx
        return sorted[lowerIdx] + (sorted[upperIdx] - sorted[lowerIdx]) * frac
    }
}
