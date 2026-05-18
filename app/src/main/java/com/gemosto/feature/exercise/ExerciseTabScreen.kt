package com.gemosto.feature.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * Tab Latihan — host untuk flow Program → Session → PostSession.
 *
 * State `tabState` disimpan lokal selama tab aktif.
 *
 * Spec: 003-exercise-recommendation.md.
 */
@Composable
fun ExerciseTabScreen(
    paddingValues: PaddingValues,
    onGoToScan: () -> Unit = {},
    viewModel: ProgramViewModel = koinViewModel(),
) {
    val programState by viewModel.activeProgramState.collectAsStateWithLifecycle()
    val genState by viewModel.generateState.collectAsStateWithLifecycle()
    val program = (programState as? ActiveProgramState.Loaded)?.program
    val isProgramLoading = programState is ActiveProgramState.Loading

    var tabState: ExerciseTabState by remember { mutableStateOf(ExerciseTabState.Program) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = tabState !is ExerciseTabState.Program) {
        // Dari Session → balik ke Program (konfirmasi tidak perlu — user
        // bisa resume lagi karena state belum tersimpan)
        tabState = ExerciseTabState.Program
    }

    when (val state = tabState) {
        ExerciseTabState.Program -> ProgramOrEmptyState(
            paddingValues = paddingValues,
            program = program,
            isProgramLoading = isProgramLoading,
            genState = genState,
            onStartSession = {
                if (program != null && program.exercises.isNotEmpty()) {
                    tabState = ExerciseTabState.Session(exerciseIndex = 0)
                }
            },
            onGoToScan = onGoToScan,
        )

        is ExerciseTabState.Session -> {
            val current = program
            if (current == null || current.exercises.isEmpty()) {
                LaunchedEffect(Unit) { tabState = ExerciseTabState.Program }
            } else {
                val safeIndex = state.exerciseIndex.coerceIn(0, current.exercises.size - 1)
                ExerciseDetailScreen(
                    paddingValues = paddingValues,
                    exercise = current.exercises[safeIndex],
                    mode = DetailMode.Session(
                        currentIndex = safeIndex,
                        totalCount = current.exercises.size,
                    ),
                    onAdvance = {
                        if (safeIndex >= current.exercises.size - 1) {
                            // Latihan terakhir -> catat pain log sebelum kembali ke Program.
                            tabState = ExerciseTabState.PostSession(stoppedDueToPain = false)
                        } else {
                            tabState = ExerciseTabState.Session(safeIndex + 1)
                        }
                    },
                    onStopForPain = { showStopConfirmDialog = true },
                    onClose = { tabState = ExerciseTabState.Program },
                )
            }
        }

        is ExerciseTabState.PostSession -> {
            PostExercisePainDialog(
                onSave = { score ->
                    viewModel.savePainLog(
                        score = score,
                        stoppedDueToPain = state.stoppedDueToPain,
                        onComplete = {
                            tabState = ExerciseTabState.Program
                        }
                    )
                }
            )
        }
    }

    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = { Text("Hentikan sesi?") },
            text = {
                Text(
                    "Yakin hentikan latihan? Kami akan minta Anda mengisi tingkat nyeri " +
                        "supaya bisa menyesuaikan latihan berikutnya.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopConfirmDialog = false
                    tabState = ExerciseTabState.PostSession(stoppedDueToPain = true)
                }) {
                    Text("Ya, hentikan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmDialog = false }) {
                    Text("Lanjutkan")
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Program / Empty / Loading state dispatcher
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ProgramOrEmptyState(
    paddingValues: PaddingValues,
    program: com.gemosto.domain.model.ExerciseProgram?,
    isProgramLoading: Boolean,
    genState: GenerateState,
    onStartSession: () -> Unit,
    onGoToScan: () -> Unit,
) {
    when {
        // Sedang generate → loading state, tapi kalau program lama masih ada
        // tampilkan saja program lama (Firestore listener masih aktif).
        genState is GenerateState.Generating && program == null -> {
            ProgramLoadingState(paddingValues = paddingValues)
        }
        isProgramLoading && program == null -> {
            ProgramLoadingState(paddingValues = paddingValues)
        }
        genState is GenerateState.Failed && program == null -> {
            ProgramEmptyState(
                paddingValues = paddingValues,
                title = "Gagal membuat program",
                body = genState.message,
                primaryCtaLabel = "Tutup",
                onPrimaryCta = { /* genState consumed automatic */ },
            )
        }
        program == null -> {
            ProgramEmptyState(
                paddingValues = paddingValues,
                title = "Belum Ada Program",
                body = "Lakukan Scan ROM dulu untuk mendapatkan rekomendasi latihan personal.",
                primaryCtaLabel = "Mulai Scan ROM",
                onPrimaryCta = onGoToScan,
            )
        }
        else -> {
            ProgramScreen(
                paddingValues = paddingValues,
                program = program,
                onStartSession = onStartSession,
                onExerciseClick = { /* Hari 11+: bisa expand jadi preview detail.
                                       MVP: skip — user mulai sesi via tombol primary */ },
            )
        }
    }
}
