package com.gemosto.feature.scan

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.gemosto.feature.common.TabPlaceholder

/**
 * Placeholder Hari 5-8 — diisi penuh saat spec 002 (ROM Scan).
 * Akan jadi: ScanIntroScreen → RomCameraScreen → RomResultScreen.
 */
@Composable
fun ScanTabScreen(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    TabPlaceholder(
        paddingValues = paddingValues,
        title = "Scan ROM",
        body = "Pengukuran rentang gerak (ROM) lutut via kamera akan diimplementasi " +
               "Hari 5-8. Ini fitur inti skripsi.",
    )
}
