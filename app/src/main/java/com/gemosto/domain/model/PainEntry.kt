package com.gemosto.domain.model

/**
 * Catatan nyeri lutut user pasca-sesi latihan.
 *
 * Disimpan di Firestore path `users/{uid}/painLogs/{painId}`.
 *
 * Field penting:
 *  - `score`: 0..10 (skala visual analog standar) — clamped saat save
 *  - `stoppedDueToPain`: true kalau user tap "Hentikan - Nyeri" mid-session
 *  - `programId`: referensi ke program saat sesi (null untuk pain log standalone)
 *
 * Spec: 004-pain-log-minimal.md.
 */
data class PainEntry(
    val id: String,
    val userId: String,
    val timestampMs: Long,
    val score: Int,
    val stoppedDueToPain: Boolean = false,
    val programId: String? = null,
    val sessionStartedAtMs: Long? = null,
    val sessionDurationMs: Long? = null,
) {
    val isHighPain: Boolean get() = score >= HIGH_PAIN_THRESHOLD
    val isVeryHighPain: Boolean get() = score >= VERY_HIGH_PAIN_THRESHOLD

    companion object {
        /** Threshold untuk "Nyeri tinggi" — pemicu safety note di rule engine. */
        const val HIGH_PAIN_THRESHOLD = 7

        /** Threshold untuk "Nyeri sangat tinggi" — pemicu block kalau berturut. */
        const val VERY_HIGH_PAIN_THRESHOLD = 8
    }
}
