package com.gemosto.domain.rom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk RomAngleCalculator.
 *
 * Coverage target per CLAUDE.md section 10: tiap branch.
 * Per spec 002 AC20, mencakup:
 *  - Kaki lurus (interior=180)
 *  - Tekuk 90°
 *  - Tekuk penuh (~30-40°)
 *  - Input degenerate → NaN
 */
class RomAngleCalculatorTest {

    private val tolerance = 0.5f  // toleransi floating point untuk acos

    // ─── computeKneeInteriorAngle ───────────────────────────────────

    @Test
    fun `kaki lurus vertikal returns approximately 180 degrees`() {
        // hip(0,0) di atas, knee(0,1) di tengah, ankle(0,2) di bawah
        // → interior 180°
        val angle = RomAngleCalculator.computeKneeInteriorAngle(
            hip = Point2D(0f, 0f),
            knee = Point2D(0f, 1f),
            ankle = Point2D(0f, 2f),
        )
        assertEquals(180f, angle, tolerance)
    }

    @Test
    fun `lutut menekuk 90 derajat returns approximately 90 degrees`() {
        // L-shape: hip(0,0) di atas, knee(0,1), ankle(1,1) ke kanan
        val angle = RomAngleCalculator.computeKneeInteriorAngle(
            hip = Point2D(0f, 0f),
            knee = Point2D(0f, 1f),
            ankle = Point2D(1f, 1f),
        )
        assertEquals(90f, angle, tolerance)
    }

    @Test
    fun `lutut menekuk penuh sekitar 30-45 derajat`() {
        // Tumit hampir mendekati paha:
        // hip(0,0) → knee(0,1) → ankle(0.3, 0.5) (kembali ke arah hip)
        val angle = RomAngleCalculator.computeKneeInteriorAngle(
            hip = Point2D(0f, 0f),
            knee = Point2D(0f, 1f),
            ankle = Point2D(0.3f, 0.5f),
        )
        assertTrue("expected angle in 25..50, got $angle", angle in 25f..50f)
    }

    @Test
    fun `degenerate input knee equals hip returns NaN`() {
        val angle = RomAngleCalculator.computeKneeInteriorAngle(
            hip = Point2D(1f, 1f),
            knee = Point2D(1f, 1f),     // sama dengan hip
            ankle = Point2D(0f, 0f),
        )
        assertTrue("expected NaN, got $angle", angle.isNaN())
    }

    @Test
    fun `degenerate input knee equals ankle returns NaN`() {
        val angle = RomAngleCalculator.computeKneeInteriorAngle(
            hip = Point2D(0f, 0f),
            knee = Point2D(1f, 1f),
            ankle = Point2D(1f, 1f),    // sama dengan knee
        )
        assertTrue("expected NaN, got $angle", angle.isNaN())
    }

    @Test
    fun `kolinear collapsed all same point returns NaN`() {
        val same = Point2D(0f, 0f)
        val angle = RomAngleCalculator.computeKneeInteriorAngle(same, same, same)
        assertTrue(angle.isNaN())
    }

    @Test
    fun `kolinear straight horizontal returns 180 degrees`() {
        // 3 titik di garis horizontal — sudut 180°
        val angle = RomAngleCalculator.computeKneeInteriorAngle(
            hip = Point2D(0f, 5f),
            knee = Point2D(2f, 5f),
            ankle = Point2D(5f, 5f),
        )
        assertEquals(180f, angle, tolerance)
    }

    @Test
    fun `lutut diagonal 45 derajat tekuk`() {
        // hip(0,0), knee(0,1), ankle(1,2) — sudut 135° (tekuk 45°)
        val angle = RomAngleCalculator.computeKneeInteriorAngle(
            hip = Point2D(0f, 0f),
            knee = Point2D(0f, 1f),
            ankle = Point2D(1f, 2f),
        )
        assertEquals(135f, angle, tolerance)
    }

    // ─── toFlexion ─────────────────────────────────────────────────

    @Test
    fun `toFlexion with 180 interior returns 0 flexion`() {
        assertEquals(0f, RomAngleCalculator.toFlexion(180f), tolerance)
    }

    @Test
    fun `toFlexion with 90 interior returns 90 flexion`() {
        assertEquals(90f, RomAngleCalculator.toFlexion(90f), tolerance)
    }

    @Test
    fun `toFlexion with 30 interior returns 150 flexion`() {
        assertEquals(150f, RomAngleCalculator.toFlexion(30f), tolerance)
    }

    @Test
    fun `toFlexion with 200 interior coerced to 0 (no negative flexion)`() {
        // Defensive: kalau ada artefak interior > 180°, flexion clamped to 0
        assertEquals(0f, RomAngleCalculator.toFlexion(200f), tolerance)
    }

    @Test
    fun `toFlexion with NaN returns NaN`() {
        assertTrue(RomAngleCalculator.toFlexion(Float.NaN).isNaN())
    }

    // ─── toExtensionLag ────────────────────────────────────────────

    @Test
    fun `toExtensionLag with 180 interior returns 0 lag`() {
        assertEquals(0f, RomAngleCalculator.toExtensionLag(180f), tolerance)
    }

    @Test
    fun `toExtensionLag with 175 interior returns 5 lag`() {
        assertEquals(5f, RomAngleCalculator.toExtensionLag(175f), tolerance)
    }
}
