package com.gemosto.feature.exercise

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.gemosto.feature.common.TabPlaceholder

/**
 * Placeholder Hari 9-11 — diisi penuh saat spec 003 (Exercise Recommendation).
 */
@Composable
fun ExerciseTabScreen(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    TabPlaceholder(
        paddingValues = paddingValues,
        title = "Latihan",
        body = "Program latihan personal Anda akan tampil di sini setelah Scan ROM. " +
               "Implementasi penuh Hari 9-11.",
    )
}
