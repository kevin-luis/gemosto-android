package com.gemosto.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.domain.model.RomResult
import com.gemosto.feature.result.RomResultScreen
import org.koin.androidx.compose.koinViewModel

/**
 * Screen utama untuk tab Riwayat ROM.
 * Mengelola state apakah menampilkan list history atau detail read-only.
 */
@Composable
fun HistoryTabScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onGoToScan: () -> Unit = {},
    viewModel: HistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedRomId by remember { mutableStateOf<String?>(null) }

    val selectedRom = remember(selectedRomId, state.items) {
        state.items.find { it.id == selectedRomId }
    }

    if (selectedRom != null) {
        BackHandler { selectedRomId = null }
        RomResultScreen(
            paddingValues = paddingValues,
            rom = selectedRom,
            readOnly = true,
            onClose = { selectedRomId = null }
        )
    } else {
        RomHistoryScreen(
            paddingValues = paddingValues,
            viewModel = viewModel,
            onStartScan = onGoToScan,
            onResultClick = { selectedRomId = it }
        )
    }
}

