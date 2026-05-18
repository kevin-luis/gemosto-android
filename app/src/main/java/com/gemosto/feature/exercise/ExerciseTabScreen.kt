package com.gemosto.feature.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.exercise.ExerciseCatalog
import com.gemosto.domain.exercise.ExerciseId
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseNarrative
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.NarrativeSource
import com.gemosto.domain.model.ProgramStatus
import com.gemosto.domain.model.RomCategory
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

@Preview(showSystemUi = true, name = "Exercise tab - program")
@Composable
private fun ExerciseTabProgramPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProgramOrEmptyState(
                paddingValues = PaddingValues(0.dp),
                program = previewExerciseTabProgram,
                isProgramLoading = false,
                genState = GenerateState.Idle,
                onStartSession = {},
                onGoToScan = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Exercise tab - empty")
@Composable
private fun ExerciseTabEmptyPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProgramOrEmptyState(
                paddingValues = PaddingValues(0.dp),
                program = null,
                isProgramLoading = false,
                genState = GenerateState.Idle,
                onStartSession = {},
                onGoToScan = {},
            )
        }
    }
}

private val previewExerciseTabProgram = ExerciseProgram(
    id = "preview-program",
    userId = "preview-user",
    generatedAt = 1L,
    basedOnRomId = "preview-rom",
    romCategory = RomCategory.MODERATE,
    level = ExerciseLevel.GENTLE,
    durationWeeks = 4,
    frequencyPerWeek = "3x/minggu",
    exercises = listOf(
        ExerciseCatalog.get(ExerciseId.QUAD_SETS),
        ExerciseCatalog.get(ExerciseId.HEEL_SLIDES),
        ExerciseCatalog.get(ExerciseId.ANKLE_PUMPS),
    ),
    narrative = ExerciseNarrative(
        intro = "Program ringan ini disusun agar lutut Anda bergerak bertahap.",
        rationale = "ROM Anda berada pada kategori sedang, sehingga latihan low-impact lebih sesuai untuk memulai.",
        weeklyMotivation = "Mulai perlahan dan catat respons nyeri setelah latihan.",
        source = NarrativeSource.FALLBACK_STATIC,
    ),
    status = ProgramStatus.ACTIVE,
)
