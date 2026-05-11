package com.gemosto.domain.exercise

import com.gemosto.domain.model.PainEntry

/**
 * Helper logic untuk pain log data yang digunakan oleh Rule Engine.
 */
object PainLogLogic {

    /**
     * Menghitung jumlah entri terbaru yang secara berturut-turut memiliki score >= threshold.
     * @param entries List dari PainEntry, harus diurutkan descending (terbaru di index 0).
     * @param threshold Skor minimal untuk dianggap "nyeri tinggi".
     */
    fun computeConsecutiveHighPainCount(entries: List<PainEntry>, threshold: Int = 8): Int {
        return entries.takeWhile { it.score >= threshold }.count()
    }
}
