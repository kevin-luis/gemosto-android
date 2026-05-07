package com.gemosto.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Gemosto typography — body min 15sp, max 3 weight (400/500/600).
 *
 * Detail: lihat `_design-system.md` section 2.
 * Aturan: TIDAK pakai bold 700+. Kalau perlu emphasis, gunakan weight 600.
 */
private val gemFont: FontFamily = FontFamily.Default

val GemostoTypography: Typography = Typography(
    // Display — splash, onboarding hero
    displayLarge = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    // Headline — heading screen
    headlineLarge = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    // Title — card title, section header
    titleLarge = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Body — content utama (MIN 15sp)
    bodyLarge = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        // Hanya untuk caption / meta-info — bukan content utama
        fontFamily = gemFont,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // Label — button, form label
    labelLarge = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = gemFont,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
