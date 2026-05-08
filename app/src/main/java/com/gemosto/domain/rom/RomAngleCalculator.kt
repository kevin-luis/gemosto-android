package com.gemosto.domain.rom

import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Titik 2D di bidang gambar (normalized koordinat MediaPipe: 0..1).
 *
 * Pure data class — tidak depend Android.
 */
data class Point2D(val x: Float, val y: Float)

/**
 * Pure-Kotlin calculator untuk sudut lutut dari 3 landmark (hip, knee, ankle).
 *
 * Convention output:
 *  - `interiorAngleDeg` (180 = lurus, 0 = mustahil tertekuk penuh, praktis flex max ~150°)
 *  - `flexionDeg` = 180 - interior (bigger = more bent)
 *  - `extensionLagDeg` = 180 - interior (bigger = less able to fully straighten)
 *    → secara matematis sama dengan flexion, perbedaannya ada di FASE pengukuran
 *      di RomSessionController (Hari 7-8): saat user diminta lurus, kita ambil
 *      MIN value (lag = ideal 0°). Saat user diminta tekuk, ambil MAX (flex max).
 *
 * Spec: 002-rom-scan.md section 6.2.
 *
 * SUMBER KEBENARAN sudut lutut. Tidak boleh diduplikasi.
 */
object RomAngleCalculator {

    /**
     * Sudut interior di vertex `knee`, dibentuk oleh vektor `knee→hip`
     * dan `knee→ankle`.
     *
     * @return derajat dalam range [0, 180]. Float.NaN kalau ada degenerate
     *         input (mis. dua titik koincide → magnitude 0).
     */
    fun computeKneeInteriorAngle(hip: Point2D, knee: Point2D, ankle: Point2D): Float {
        val v1x = hip.x - knee.x
        val v1y = hip.y - knee.y
        val v2x = ankle.x - knee.x
        val v2y = ankle.y - knee.y

        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)
        if (mag1 == 0f || mag2 == 0f) return Float.NaN

        val dot = v1x * v2x + v1y * v2y
        val cos = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    /**
     * Konversi sudut interior → derajat flexion (semakin besar = semakin tertekuk).
     * Bila interior = 180 (lurus penuh), flexion = 0.
     * Bila interior = 90 (siku-siku), flexion = 90.
     */
    fun toFlexion(interiorDeg: Float): Float {
        if (interiorDeg.isNaN()) return Float.NaN
        return (180f - interiorDeg).coerceAtLeast(0f)
    }

    /**
     * Konversi sudut interior → derajat extension lag.
     * Identik dengan toFlexion secara nilai; perbedaan kontekstual ada di
     * FASE pengukuran (lihat doc class).
     */
    fun toExtensionLag(interiorDeg: Float): Float = toFlexion(interiorDeg)
}
