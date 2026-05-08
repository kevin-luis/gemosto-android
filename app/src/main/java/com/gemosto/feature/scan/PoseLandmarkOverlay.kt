package com.gemosto.feature.scan

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.data.pose.PoseDetection
import com.gemosto.data.pose.PoseDetector
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.rom.Point2D
import kotlin.math.min

/**
 * Overlay yang menggambar 3 landmark + 2 garis (hip → knee → ankle) di atas
 * camera preview.
 *
 * Color coding berdasar visibility (per spec 002 section 4):
 *  - Hijau (visibility ≥ 0.7): pose terdeteksi jelas
 *  - Kuning (visibility 0.4..0.7): confidence menengah
 *  - Merah (visibility < 0.4): tidak yakin
 *
 * Coordinate transform (FILL_CENTER):
 *  - PreviewView pakai FILL_CENTER → image scaled untuk fill view, crop excess
 *  - Sama dengan transform di sini supaya landmark align dengan visual
 */
@Composable
fun PoseLandmarkOverlay(
    kneeSide: KneeSide,
    poseDetector: PoseDetector,
    modifier: Modifier = Modifier,
) {
    val pose by poseDetector.detections.collectAsStateWithLifecycle(
        initialValue = PoseDetection.EMPTY,
    )
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { 8.dp.toPx() }
    val lineWidthPx = with(density) { 4.dp.toPx() }
    val haloRadiusPx = with(density) { 12.dp.toPx() }

    Canvas(modifier = modifier) {
        if (pose.isEmpty() || pose.imageWidth == 0 || pose.imageHeight == 0) return@Canvas

        val (hipIdx, kneeIdx, ankleIdx) = when (kneeSide) {
            KneeSide.LEFT -> Triple(
                PoseDetection.LEFT_HIP,
                PoseDetection.LEFT_KNEE,
                PoseDetection.LEFT_ANKLE,
            )
            KneeSide.RIGHT -> Triple(
                PoseDetection.RIGHT_HIP,
                PoseDetection.RIGHT_KNEE,
                PoseDetection.RIGHT_ANKLE,
            )
            KneeSide.BOTH -> return@Canvas
        }

        val hip = pose.landmarks.getOrNull(hipIdx) ?: return@Canvas
        val knee = pose.landmarks.getOrNull(kneeIdx) ?: return@Canvas
        val ankle = pose.landmarks.getOrNull(ankleIdx) ?: return@Canvas

        val hipVis = pose.visibilityOf(hipIdx)
        val kneeVis = pose.visibilityOf(kneeIdx)
        val ankleVis = pose.visibilityOf(ankleIdx)

        // ─── FILL_CENTER transform ───────────────────────────────
        val viewW = size.width
        val viewH = size.height
        val imgW = pose.imageWidth.toFloat()
        val imgH = pose.imageHeight.toFloat()
        val viewAspect = viewW / viewH
        val imgAspect = imgW / imgH

        val scale: Float
        val offsetX: Float
        val offsetY: Float
        if (imgAspect > viewAspect) {
            // Image lebih lebar — height fills, width crop
            scale = viewH / imgH
            offsetX = (viewW - imgW * scale) / 2f
            offsetY = 0f
        } else {
            // Image lebih tinggi — width fills, height crop
            scale = viewW / imgW
            offsetX = 0f
            offsetY = (viewH - imgH * scale) / 2f
        }

        fun project(p: Point2D): Offset = Offset(
            x = p.x * imgW * scale + offsetX,
            y = p.y * imgH * scale + offsetY,
        )

        val hipScreen = project(hip)
        val kneeScreen = project(knee)
        val ankleScreen = project(ankle)

        // ─── Draw 2 lines ────────────────────────────────────────
        drawLine(
            color = colorForVisibility(min(hipVis, kneeVis)),
            start = hipScreen,
            end = kneeScreen,
            strokeWidth = lineWidthPx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colorForVisibility(min(kneeVis, ankleVis)),
            start = kneeScreen,
            end = ankleScreen,
            strokeWidth = lineWidthPx,
            cap = StrokeCap.Round,
        )

        // ─── Draw 3 dots (with halo) ─────────────────────────────
        drawDot(hipScreen, hipVis, dotRadiusPx, haloRadiusPx)
        drawDot(kneeScreen, kneeVis, dotRadiusPx * 1.3f, haloRadiusPx * 1.3f)  // knee bigger
        drawDot(ankleScreen, ankleVis, dotRadiusPx, haloRadiusPx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDot(
    center: Offset,
    visibility: Float,
    radiusPx: Float,
    haloRadiusPx: Float,
) {
    val color = colorForVisibility(visibility)
    // Halo (semi-transparent ring) untuk readability di background apa pun
    drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = haloRadiusPx,
        center = center,
    )
    // Outline putih supaya contrast di background terang/gelap
    drawCircle(
        color = Color.White,
        radius = radiusPx + 2f,
        center = center,
        style = Stroke(width = 2f),
    )
    // Dot solid
    drawCircle(
        color = color,
        radius = radiusPx,
        center = center,
    )
}

private fun colorForVisibility(visibility: Float): Color = when {
    visibility >= 0.7f -> Color(0xFF4ADE80)    // Hijau
    visibility >= 0.4f -> Color(0xFFFBBF24)    // Kuning
    else -> Color(0xFFF87171)                  // Merah
}
