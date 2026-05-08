package com.gemosto.feature.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

/**
 * Tab Scan — wrapper untuk flow Intro → Camera (→ Result Hari 8).
 *
 * State `flow` disimpan di rememberSaveable sehingga survive config change
 * tapi reset saat user pindah tab.
 */
@Composable
fun ScanTabScreen(
    paddingValues: PaddingValues,
    profile: UserProfile,
    viewModel: ScanViewModel = koinViewModel(),
) {
    LaunchedEffect(profile.affectedKnee) {
        viewModel.initFromProfile(profile.affectedKnee)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Saved state survive rotation; reset saat tab swap (composable removed).
    var flow: ScanFlowState by rememberSaveable(
        stateSaver = scanFlowSaver(),
    ) { mutableStateOf(ScanFlowState.Intro) }

    // Back handler: Camera → Intro, Intro → no-op (sistem handle).
    BackHandler(enabled = flow == ScanFlowState.Camera) {
        flow = ScanFlowState.Intro
    }

    when (flow) {
        ScanFlowState.Intro -> ScanIntroScreen(
            paddingValues = paddingValues,
            selectedKneeSide = state.selectedKneeSide,
            onKneeSelected = viewModel::onKneeSelected,
            onReadyClick = { flow = ScanFlowState.Camera },
        )
        ScanFlowState.Camera -> {
            val side = state.selectedKneeSide
            if (side != null) {
                RomCameraScreen(
                    kneeSide = side,
                    onClose = { flow = ScanFlowState.Intro },
                    viewModel = viewModel,
                )
            } else {
                // Defensive: jangan tampil camera tanpa knee side.
                flow = ScanFlowState.Intro
            }
        }
    }
}

/**
 * Saver untuk `ScanFlowState` supaya bisa disimpan di Bundle.
 * Sealed object → simpan nama class.
 */
private fun scanFlowSaver() = androidx.compose.runtime.saveable.Saver<ScanFlowState, String>(
    save = {
        when (it) {
            ScanFlowState.Intro -> "Intro"
            ScanFlowState.Camera -> "Camera"
        }
    },
    restore = {
        when (it) {
            "Camera" -> ScanFlowState.Camera
            else -> ScanFlowState.Intro
        }
    },
)
