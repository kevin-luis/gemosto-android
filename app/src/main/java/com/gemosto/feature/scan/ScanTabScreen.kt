package com.gemosto.feature.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.domain.model.UserProfile
import com.gemosto.feature.exercise.ProgramViewModel
import com.gemosto.feature.result.RomResultScreen
import org.koin.androidx.compose.koinViewModel

/**
 * Tab Scan — wrapper untuk flow Intro → Camera → Result.
 *
 * State `flow` disimpan via rememberSaveable, survive config change,
 * reset saat user pindah tab atau setelah Result.
 *
 * @param onGoToExerciseTab dipanggil saat user tap "Lihat Rekomendasi Latihan"
 *        di RomResultScreen — switch tab ke Exercise + program di-generate.
 */
@Composable
fun ScanTabScreen(
    paddingValues: PaddingValues,
    profile: UserProfile,
    onGoToExerciseTab: () -> Unit = {},
    viewModel: ScanViewModel = koinViewModel(),
    programViewModel: ProgramViewModel = koinViewModel(),
) {
    LaunchedEffect(profile.affectedKnee) {
        viewModel.initFromProfile(profile.affectedKnee)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    var flow: ScanFlowState by rememberSaveable(stateSaver = scanFlowSaver()) {
        mutableStateOf(ScanFlowState.Intro)
    }

    BackHandler(enabled = flow == ScanFlowState.Camera) {
        viewModel.resetSession()
        flow = ScanFlowState.Intro
    }

    when (val current = flow) {
        ScanFlowState.Intro -> ScanIntroScreen(
            paddingValues = paddingValues,
            selectedKneeSide = state.selectedKneeSide,
            onKneeSelected = viewModel::onKneeSelected,
            onReadyClick = {
                viewModel.resetSession()
                flow = ScanFlowState.Camera
            },
        )
        ScanFlowState.Camera -> {
            val side = state.selectedKneeSide
            if (side != null) {
                RomCameraScreen(
                    paddingValues = paddingValues,
                    kneeSide = side,
                    onClose = {
                        viewModel.resetSession()
                        flow = ScanFlowState.Intro
                    },
                    onSessionDone = { romId ->
                        flow = ScanFlowState.Result(romId)
                    },
                    viewModel = viewModel,
                )
            } else {
                flow = ScanFlowState.Intro
            }
        }
        is ScanFlowState.Result -> {
            val rom = state.completedRom
            if (rom != null && rom.id == current.romId) {
                RomResultScreen(
                    paddingValues = paddingValues,
                    rom = rom,
                    onScanAgain = {
                        viewModel.resetSession()
                        flow = ScanFlowState.Intro
                    },
                    onSeeRecommendation = {
                        // 1. Trigger generate program berdasar ROM ini
                        programViewModel.generateProgram(romId = current.romId)
                        // 2. Reset scan flow supaya tab Scan kembali bersih
                        viewModel.resetSession()
                        flow = ScanFlowState.Intro
                        // 3. Switch ke tab Exercise — user lihat program loading → loaded
                        onGoToExerciseTab()
                    },
                )
            } else {
                LaunchedEffect(Unit) {
                    viewModel.resetSession()
                    flow = ScanFlowState.Intro
                }
            }
        }
    }
}

private fun scanFlowSaver(): Saver<ScanFlowState, Any> = Saver(
    save = { state ->
        when (state) {
            ScanFlowState.Intro -> "Intro"
            ScanFlowState.Camera -> "Camera"
            is ScanFlowState.Result -> "Result|${state.romId}"
        }
    },
    restore = { value ->
        val str = value as? String ?: return@Saver ScanFlowState.Intro
        when {
            str == "Camera" -> ScanFlowState.Camera
            str.startsWith("Result|") -> ScanFlowState.Result(str.removePrefix("Result|"))
            else -> ScanFlowState.Intro
        }
    },
)
