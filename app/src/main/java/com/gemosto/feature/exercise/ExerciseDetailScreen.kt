package com.gemosto.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.exercise.Exercise
import com.gemosto.domain.exercise.ExerciseCatalog
import com.gemosto.domain.exercise.ExerciseId
import com.gemosto.domain.model.ExerciseLevel

/**
 * Layar detail satu latihan — dipakai dalam 2 mode:
 *
 *  1. Preview (dari ProgramScreen tap card) — `mode = Preview`
 *     Tombol footer: "Tutup". Tidak track session progress.
 *
 *  2. Sequential session (mulai dari "Mulai Sesi Latihan") — `mode = Session`
 *     Step indicator "Latihan N dari M".
 *     Tombol primary "Selesai, Lanjut" / "Selesaikan Sesi" (latihan terakhir).
 *     Tombol danger "Hentikan - Nyeri" — fixed bottom, akses cepat.
 *
 * Spec: 003-exercise-recommendation.md section 4 — ExerciseDetailScreen.
 */
@Composable
fun ExerciseDetailScreen(
    paddingValues: PaddingValues,
    exercise: Exercise,
    mode: DetailMode,
    onAdvance: () -> Unit,
    onStopForPain: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (mode is DetailMode.Session) {
                StepIndicator(
                    current = mode.currentIndex + 1,
                    total = mode.totalCount,
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(16.dp))
            }

            HeroIllustration()

            Spacer(Modifier.height(16.dp))

            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W600),
            )

            Spacer(Modifier.height(8.dp))

            ParameterChipRow(exercise = exercise)

            Spacer(Modifier.height(20.dp))

            DescriptionSection(items = exercise.description)
            Spacer(Modifier.height(16.dp))
            TipsSection(items = exercise.tips)
            Spacer(Modifier.height(16.dp))
            WarningsSection(items = exercise.warnings)

            Spacer(Modifier.height(24.dp))
        }

        FooterActions(
            mode = mode,
            onAdvance = onAdvance,
            onStopForPain = onStopForPain,
            onClose = onClose,
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Mode (Preview vs Session)
// ─────────────────────────────────────────────────────────────────

sealed interface DetailMode {
    data object Preview : DetailMode
    data class Session(val currentIndex: Int, val totalCount: Int) : DetailMode {
        val isLast: Boolean get() = currentIndex == totalCount - 1
    }
}

// ─────────────────────────────────────────────────────────────────
// Step indicator (sequential mode only)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = "Latihan $current dari $total",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = GemColors.EmeraldPrimary,
            trackColor = GemColors.Border,
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Hero ilustrasi placeholder
// ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroIllustration() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                color = GemColors.EmeraldLight,
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            tint = GemColors.EmeraldPrimary,
            modifier = Modifier.size(80.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Parameter chip row — sets, reps, rest
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParameterChipRow(exercise: Exercise) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ParamChip("Set", "${exercise.sets}×")
        ParamChip("Reps", "${exercise.reps}")
        ParamChip("Istirahat", "${exercise.restSeconds}s")
        ParamChip("Estimasi", "~${exercise.estimatedMinutes} mnt")
    }
}

@Composable
private fun ParamChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = GemColors.BackgroundSoft,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = GemColors.EmeraldDark,
                    fontWeight = FontWeight.W600,
                ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Description / Tips / Warnings sections
// ─────────────────────────────────────────────────────────────────

@Composable
private fun DescriptionSection(items: List<String>) {
    Column {
        Text(
            text = "Cara Melakukan",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = CircleShape,
                    color = GemColors.EmeraldPrimary,
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GemColors.OnPrimary,
                                fontWeight = FontWeight.W600,
                            ),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TipsSection(items: List<String>) {
    SectionWithIcon(
        title = "Tips",
        items = items,
        iconColor = GemColors.EmeraldPrimary,
        iconResolver = { Icons.Outlined.CheckCircle },
    )
}

@Composable
private fun WarningsSection(items: List<String>) {
    SectionWithIcon(
        title = "Hentikan jika",
        items = items,
        iconColor = GemColors.Warning,
        iconResolver = { Icons.Outlined.WarningAmber },
    )
}

@Composable
private fun SectionWithIcon(
    title: String,
    items: List<String>,
    iconColor: Color,
    iconResolver: () -> androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = iconResolver(),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Footer actions — tergantung mode
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FooterActions(
    mode: DetailMode,
    onAdvance: () -> Unit,
    onStopForPain: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (mode) {
                DetailMode.Preview -> {
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("Tutup", style = MaterialTheme.typography.labelLarge)
                    }
                }
                is DetailMode.Session -> {
                    Button(
                        onClick = onAdvance,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = if (mode.isLast) "Selesaikan Sesi" else "Selesai, Lanjut",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onStopForPain,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GemColors.Danger,
                            contentColor = GemColors.OnPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Hentikan - Nyeri",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────

@Preview(showSystemUi = true, name = "Session — middle")
@Composable
private fun ExerciseDetailSessionPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExerciseDetailScreen(
                paddingValues = PaddingValues(0.dp),
                exercise = ExerciseCatalog.get(ExerciseId.STRAIGHT_LEG_RAISE),
                mode = DetailMode.Session(currentIndex = 1, totalCount = 5),
                onAdvance = {},
                onStopForPain = {},
                onClose = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Session — last")
@Composable
private fun ExerciseDetailLastPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExerciseDetailScreen(
                paddingValues = PaddingValues(0.dp),
                exercise = ExerciseCatalog.get(ExerciseId.WALL_SIT_SHORT),
                mode = DetailMode.Session(currentIndex = 4, totalCount = 5),
                onAdvance = {},
                onStopForPain = {},
                onClose = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Preview mode")
@Composable
private fun ExerciseDetailPreviewModePreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExerciseDetailScreen(
                paddingValues = PaddingValues(0.dp),
                exercise = ExerciseCatalog.get(ExerciseId.QUAD_SETS),
                mode = DetailMode.Preview,
                onAdvance = {},
                onStopForPain = {},
                onClose = {},
            )
        }
    }
}

// suppress unused
@Suppress("unused")
private val unusedLevel: ExerciseLevel? = null
