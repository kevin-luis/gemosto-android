package com.gemosto.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Gemosto color tokens — SUMBER KEBENARAN warna untuk seluruh app.
 *
 * Detail palette: lihat `_design-system.md` di repo spec.
 * Aturan: TIDAK boleh hardcode hex di Composable lain — selalu pakai
 * `MaterialTheme.colorScheme.*` atau import token dari file ini.
 */
object GemColors {
    // Brand
    val EmeraldPrimary = Color(0xFF0F6E56)
    val EmeraldDark = Color(0xFF085041)
    val EmeraldLight = Color(0xFFE1F5EE)
    val OnPrimary = Color(0xFFFFFFFF)

    // Neutral
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF5F5E5A)
    val BackgroundDefault = Color(0xFFFFFFFF)
    val BackgroundSoft = Color(0xFFFAFAF7)
    val InputBg = Color(0xFFF1EFE8)
    val Border = Color(0xFFE8E8E3)

    // Semantic
    val Warning = Color(0xFFEF9F27)
    val Danger = Color(0xFFA32D2D)
    val DangerBg = Color(0xFFFBEAEA)
}
