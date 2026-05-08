package com.gemosto.domain.rom

import com.gemosto.domain.model.RomCategory

/**
 * Pure-Kotlin classifier untuk hasil ROM.
 *
 * Threshold (DRAFT — validasi dengan jurnal Bab 2):
 *   - NORMAL:   flexion >= 130° && extLag <= 5°
 *   - MILD:     flexion 110..129° || extLag 6..10°
 *   - MODERATE: flexion 90..109°  || extLag 11..20°
 *   - SEVERE:   flexion < 90°     || extLag > 20°
 *
 * Logika "OR" antara dua dimensi → ambil kategori paling buruk.
 * Contoh: flexion 135° (NORMAL) + extLag 7° (MILD) → kategori MILD.
 *
 * Spec: 002-rom-scan.md section 6.4.
 */
object RomClassifier {

    fun classify(maxFlexionDeg: Float, extensionLagDeg: Float): RomCategory {
        val flexCat = classifyByFlexion(maxFlexionDeg)
        val lagCat = classifyByExtensionLag(extensionLagDeg)
        // Ambil yang paling parah (ordinal lebih besar di enum = lebih buruk).
        return if (flexCat.ordinal >= lagCat.ordinal) flexCat else lagCat
    }

    private fun classifyByFlexion(flexionDeg: Float): RomCategory = when {
        flexionDeg >= 130f -> RomCategory.NORMAL
        flexionDeg >= 110f -> RomCategory.MILD
        flexionDeg >= 90f -> RomCategory.MODERATE
        else -> RomCategory.SEVERE
    }

    private fun classifyByExtensionLag(extLagDeg: Float): RomCategory = when {
        extLagDeg <= 5f -> RomCategory.NORMAL
        extLagDeg <= 10f -> RomCategory.MILD
        extLagDeg <= 20f -> RomCategory.MODERATE
        else -> RomCategory.SEVERE
    }
}
