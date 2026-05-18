package com.gemosto.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.exercise.Exercise
import com.gemosto.domain.exercise.ExerciseCatalog
import com.gemosto.domain.exercise.ExerciseId
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseNarrative
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.NarrativeSource
import com.gemosto.domain.model.ProgramStatus
import com.gemosto.domain.model.RomCategory

/**
 * Layar Program latihan.
 *
 * Spec: 003-exercise-recommendation.md section 4 — ProgramScreen.
 *
 * State variants:
 *  - BLOCK / BLOCKED_PAIN → card warning danger, no "Mulai" button
 *  - normal program → hero + narrative + exercise list + "Mulai Sesi"
 *  - narrative null → tampilkan "Memuat narasi..." (Gemini call still running)
 */
@Composable
fun ProgramScreen(
    paddingValues: PaddingValues,
    program: ExerciseProgram,
    onStartSession: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
) {
    val isBlocked = program.level == ExerciseLevel.BLOCK
        || program.status == ProgramStatus.BLOCKED_PAIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Program Latihan",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        if (isBlocked) {
            BlockedCard(safetyNote = program.safetyNote)
            Spacer(Modifier.height(16.dp))
        } else {
            HeroCard(program = program)
            Spacer(Modifier.height(12.dp))
            NarrativeSection(narrative = program.narrative, level = program.level)
            Spacer(Modifier.height(16.dp))
            ExerciseListSection(
                exercises = program.exercises,
                onExerciseClick = onExerciseClick,
            )
            Spacer(Modifier.height(16.dp))
        }

        DisclaimerBanner()

        Spacer(Modifier.height(24.dp))

        if (!isBlocked) {
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Mulai Sesi Latihan", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Hero card — level, ROM kategori basis, durasi, frekuensi
// ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(program: ExerciseProgram) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GemColors.EmeraldPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Program ${program.level.displayLabel}",
                style = MaterialTheme.typography.headlineSmall.copy(color = GemColors.OnPrimary),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Berdasarkan ROM Anda — kategori ${program.romCategory.displayLabel}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = GemColors.OnPrimary.copy(alpha = 0.9f),
                ),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaChip(
                    label = "${program.durationWeeks} minggu",
                )
                Spacer(Modifier.width(8.dp))
                MetaChip(label = program.frequencyPerWeek)
                Spacer(Modifier.width(8.dp))
                MetaChip(label = "${program.exercises.size} latihan")
            }
        }
    }
}

@Composable
private fun MetaChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.18f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = GemColors.OnPrimary),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Narrative section — Gemini intro + rationale
// ─────────────────────────────────────────────────────────────────

@Composable
private fun NarrativeSection(
    narrative: ExerciseNarrative?,
    level: ExerciseLevel,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GemColors.EmeraldLight,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = GemColors.EmeraldDark,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Pesan Untuk Anda",
                    style = MaterialTheme.typography.labelMedium.copy(color = GemColors.EmeraldDark),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (narrative == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = GemColors.EmeraldDark,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Menyiapkan narasi personal...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GemColors.EmeraldDark.copy(alpha = 0.7f),
                        ),
                    )
                }
            } else {
                Text(
                    text = narrative.intro,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = GemColors.TextPrimary,
                        fontWeight = FontWeight.W500,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = narrative.rationale,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (narrative.weeklyMotivation.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "💡 ${narrative.weeklyMotivation}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GemColors.EmeraldDark,
                            fontWeight = FontWeight.W500,
                        ),
                    )
                }
                if (narrative.source == NarrativeSource.FALLBACK_STATIC) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "(narasi cadangan — koneksi terbatas)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GemColors.TextSecondary,
                        ),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Exercise list section
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ExerciseListSection(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
) {
    Column {
        Text(
            text = "Latihan dalam Program",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        exercises.forEach { exercise ->
            ExerciseRowCard(exercise = exercise, onClick = { onExerciseClick(exercise) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExerciseRowCard(exercise: Exercise, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, GemColors.Border, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = GemColors.EmeraldLight,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = GemColors.EmeraldDark,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${exercise.sets} set × ${exercise.reps} reps · ~${exercise.estimatedMinutes} mnt",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Blocked card — kategori SEVERE atau pain block
// ─────────────────────────────────────────────────────────────────

@Composable
private fun BlockedCard(safetyNote: String?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GemColors.DangerBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GemColors.Danger, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = GemColors.Danger,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Konsultasi Dokter Disarankan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = GemColors.Danger,
                        fontWeight = FontWeight.W600,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = safetyNote
                    ?: "Berdasarkan kondisi Anda, kami sarankan konsultasi tenaga medis " +
                    "sebelum memulai program latihan rumahan.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Disclaimer banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun DisclaimerBanner() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GemColors.BackgroundSoft,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GemColors.Border, RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = GemColors.TextSecondary,
                modifier = Modifier.size(16.dp).padding(top = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Program ini disusun rule-based berdasar ROM Anda. " +
                    "Bukan pengganti diagnosis dokter — hentikan jika muncul nyeri tajam " +
                    "dan konsultasi tenaga medis.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = GemColors.TextSecondary,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Empty / Loading states (caller's responsibility — this just for preview)
// ─────────────────────────────────────────────────────────────────

@Composable
fun ProgramEmptyState(
    paddingValues: PaddingValues,
    title: String,
    body: String,
    primaryCtaLabel: String? = null,
    onPrimaryCta: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = GemColors.TextSecondary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                textAlign = TextAlign.Center,
            )
            if (primaryCtaLabel != null && onPrimaryCta != null) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onPrimaryCta,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(primaryCtaLabel)
                }
            }
        }
    }
}

@Composable
fun ProgramLoadingState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = GemColors.EmeraldPrimary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Menyiapkan program Anda...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────

@Preview(showSystemUi = true, name = "Strengthening")
@Composable
private fun ProgramStrengtheningPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProgramScreen(
                paddingValues = PaddingValues(0.dp),
                program = previewProgram(
                    level = ExerciseLevel.STRENGTHENING,
                    cat = RomCategory.MILD,
                    narrative = ExerciseNarrative(
                        intro = "Selamat, Anda siap meningkatkan kekuatan lutut.",
                        rationale = "ROM Anda dalam kategori ringan, memungkinkan latihan penguatan terkontrol. " +
                            "Program ini fokus pada otot quadriceps dan hamstring untuk stabilitas sendi.",
                        weeklyMotivation = "30 menit setiap sesi membuat perbedaan nyata.",
                        source = NarrativeSource.GEMINI,
                    ),
                ),
                onStartSession = {},
                onExerciseClick = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Block")
@Composable
private fun ProgramBlockedPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProgramScreen(
                paddingValues = PaddingValues(0.dp),
                program = previewProgram(
                    level = ExerciseLevel.BLOCK,
                    cat = RomCategory.SEVERE,
                    narrative = null,
                ).copy(
                    safetyNote = "Berdasarkan ROM Anda yang sangat terbatas, kami sarankan " +
                        "konsultasi dokter sebelum mulai latihan rumahan.",
                ),
                onStartSession = {},
                onExerciseClick = {},
            )
        }
    }
}

private fun previewProgram(
    level: ExerciseLevel,
    cat: RomCategory,
    narrative: ExerciseNarrative?,
): ExerciseProgram {
    val ids = when (level) {
        ExerciseLevel.GENTLE -> listOf(
            ExerciseId.QUAD_SETS,
            ExerciseId.HEEL_SLIDES,
            ExerciseId.ANKLE_PUMPS,
            ExerciseId.GLUTE_SQUEEZE,
        )
        ExerciseLevel.STRENGTHENING -> listOf(
            ExerciseId.QUAD_SETS,
            ExerciseId.STRAIGHT_LEG_RAISE,
            ExerciseId.SHORT_ARC_QUAD,
        )
        ExerciseLevel.FUNCTIONAL -> listOf(
            ExerciseId.WALL_SQUAT,
            ExerciseId.STEP_UPS_LOW,
            ExerciseId.BRIDGES,
        )
        ExerciseLevel.BLOCK -> emptyList()
    }
    return ExerciseProgram(
        id = "preview",
        userId = "u",
        generatedAt = System.currentTimeMillis(),
        basedOnRomId = "r",
        romCategory = cat,
        level = level,
        durationWeeks = 4,
        frequencyPerWeek = "3-4x/minggu",
        exercises = ids.map(ExerciseCatalog::get),
        narrative = narrative,
        status = if (level == ExerciseLevel.BLOCK) ProgramStatus.BLOCKED_PAIN else ProgramStatus.ACTIVE,
    )
}
