package com.gemosto.feature.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.gemosto.feature.common.TabPlaceholder

/**
 * Placeholder Hari 13 — diisi penuh saat spec 007 (ROM History).
 */
@Composable
fun HistoryTabScreen(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    TabPlaceholder(
        paddingValues = paddingValues,
        title = "Riwayat ROM",
        body = "Daftar hasil scan ROM Anda akan tampil di sini (list view sederhana). " +
               "Implementasi Hari 13.",
    )
}
