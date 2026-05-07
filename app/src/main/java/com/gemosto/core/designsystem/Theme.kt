package com.gemosto.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Gemosto theme. MVP = light only.
 * Dark mode ditunda V2 (lihat `_design-system.md` section 12).
 */

private val LightColors = lightColorScheme(
    primary = GemColors.EmeraldPrimary,
    onPrimary = GemColors.OnPrimary,
    primaryContainer = GemColors.EmeraldLight,
    onPrimaryContainer = GemColors.EmeraldDark,
    secondary = GemColors.EmeraldDark,
    onSecondary = GemColors.OnPrimary,
    background = GemColors.BackgroundDefault,
    onBackground = GemColors.TextPrimary,
    surface = GemColors.BackgroundDefault,
    onSurface = GemColors.TextPrimary,
    surfaceVariant = GemColors.BackgroundSoft,
    onSurfaceVariant = GemColors.TextSecondary,
    outline = GemColors.Border,
    error = GemColors.Danger,
    errorContainer = GemColors.DangerBg,
    onError = GemColors.OnPrimary,
)

@Composable
fun GemostoTheme(
    @Suppress("UNUSED_PARAMETER")
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // MVP: tetap pakai LightColors meski system dark.
    // V2: tambah DarkColors + branching.
    MaterialTheme(
        colorScheme = LightColors,
        typography = GemostoTypography,
        content = content,
    )
}
