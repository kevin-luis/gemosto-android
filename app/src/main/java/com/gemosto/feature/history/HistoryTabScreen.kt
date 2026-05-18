package com.gemosto.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.RomCategory
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

@Preview(showSystemUi = true, name = "History tab - detail")
@Composable
private fun HistoryTabDetailPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RomResultScreen(
                paddingValues = PaddingValues(0.dp),
                rom = previewHistoryDetailRom,
                readOnly = true,
                onClose = {},
            )
        }
    }
}

private val previewHistoryDetailRom = RomResult(
    id = "rom-preview",
    userId = "preview-user",
    timestampMs = 0L,
    kneeSide = KneeSide.RIGHT,
    maxFlexionDeg = 96f,
    maxExtensionLagDeg = 14f,
    category = RomCategory.MODERATE,
    sessionDurationMs = 12000L,
    deviceModel = "Preview",
    mediaPipeModel = "preview",
)
