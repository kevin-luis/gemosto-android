package com.gemosto.domain.model

/**
 * Kategori ROM lutut hasil klasifikasi rule-based.
 *
 * Threshold per spec 002 section 6.4 — DRAFT, validasi dengan jurnal Bab 2.
 * - NORMAL: flexion ≥ 130° && extLag ≤ 5°
 * - MILD: flexion 110-129° || extLag 6-10°
 * - MODERATE: flexion 90-109° || extLag 11-20°
 * - SEVERE: flexion < 90° || extLag > 20°
 */
enum class RomCategory(val displayLabel: String) {
    NORMAL("Normal"),
    MILD("Ringan"),
    MODERATE("Sedang"),
    SEVERE("Berat"),
}

/**
 * Hasil pengukuran ROM lutut single session.
 *
 * Disimpan di Firestore path `users/{uid}/romResults/{romId}`.
 * Spec lengkap: 002-rom-scan.md section 5.
 */
data class RomResult(
    val id: String,
    val userId: String,
    val timestampMs: Long,
    val kneeSide: KneeSide,            // LEFT atau RIGHT (BOTH tidak valid di sini)
    val maxFlexionDeg: Float,
    val maxExtensionLagDeg: Float,
    val category: RomCategory,
    val sessionDurationMs: Long,
    val deviceModel: String = "",
    val mediaPipeModel: String = "",
)
