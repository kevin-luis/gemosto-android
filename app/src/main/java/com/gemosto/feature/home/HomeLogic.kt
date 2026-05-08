package com.gemosto.feature.home

import com.gemosto.domain.model.RomResult

/**
 * Pure logic helpers untuk Home — supaya mudah di-unit-test
 * dan terpisah dari Compose state.
 */
object HomeLogic {

    private const val HOUR_MS = 60L * 60 * 1000
    private const val DAY_MS = 24L * HOUR_MS

    /**
     * Pick greeting line berdasar state (latest scan + recent high pain).
     *
     * Variations (per spec 006 section 4):
     *  - First time (no scan): "Selamat datang di Gemosto"
     *  - Recent high pain (≥7) di 24 jam: "Semoga Anda merasa lebih baik hari ini."
     *  - Last scan ≥ 7 hari: "Sudah seminggu — saatnya cek ROM lagi?"
     *  - Default returning: "Bagaimana kabar lutut Anda hari ini?"
     */
    fun pickGreeting(
        latestRomTimestampMs: Long?,
        recentHighPainTimestampMs: Long?,
        nowMs: Long,
    ): GreetingKind {
        if (latestRomTimestampMs == null) return GreetingKind.FirstTime

        val recentHighPain = recentHighPainTimestampMs != null
            && (nowMs - recentHighPainTimestampMs) < DAY_MS
        if (recentHighPain) return GreetingKind.RecoverEncouragement

        val daysSinceScan = (nowMs - latestRomTimestampMs) / DAY_MS
        return if (daysSinceScan >= 7) {
            GreetingKind.WeeklyCheckReminder
        } else {
            GreetingKind.ReturningDefault
        }
    }

    /**
     * Pilih tip statis untuk hari ini (rotating berdasar tanggal).
     * Pure → deterministic untuk hari yang sama.
     */
    fun pickTipIndexForToday(nowMs: Long, tipsCount: Int): Int {
        if (tipsCount <= 0) return 0
        val dayIndex = (nowMs / DAY_MS).toInt()
        return ((dayIndex % tipsCount) + tipsCount) % tipsCount
    }

    /**
     * Format relative time dalam Bahasa Indonesia.
     * Contoh:
     *   30 detik → "baru saja"
     *   12 menit → "12 menit yang lalu"
     *   3 jam → "3 jam yang lalu"
     *   2 hari → "2 hari yang lalu"
     *   3 minggu → "3 minggu yang lalu"
     *   2 bulan → "2 bulan yang lalu"
     *   1 tahun → "1 tahun yang lalu"
     */
    fun relativeTime(timestampMs: Long, nowMs: Long): String {
        val diff = (nowMs - timestampMs).coerceAtLeast(0)
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            minutes < 1 -> "baru saja"
            minutes < 60 -> "$minutes menit yang lalu"
            hours < 24 -> "$hours jam yang lalu"
            days < 7 -> "$days hari yang lalu"
            days < 30 -> "$weeks minggu yang lalu"
            days < 365 -> "$months bulan yang lalu"
            else -> "$years tahun yang lalu"
        }
    }
}

/**
 * Tipe greeting — UI yang map ke string.
 */
enum class GreetingKind {
    FirstTime,
    ReturningDefault,
    WeeklyCheckReminder,
    RecoverEncouragement,
}
