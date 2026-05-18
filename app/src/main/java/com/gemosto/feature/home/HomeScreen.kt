package com.gemosto.feature.home

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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.ProgramStatus
import com.gemosto.domain.model.RomCategory
import com.gemosto.domain.model.RomResult
import com.gemosto.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

/**
 * Home Dashboard — entry point setelah login.
 *
 * 3 card utama:
 *  1. Scan ROM (CTA primary)
 *  2. Program Saya (kalau ada program aktif)
 *  3. Riwayat ROM Terbaru (kalau ada minimal 1 scan)
 *
 * Spec: 006-home.md
 */
@Composable
fun HomeScreen(
    profile: UserProfile,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onStartScan: () -> Unit = {},
    onOpenProgram: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeContent(
        profile = profile,
        state = state,
        paddingValues = paddingValues,
        nowMs = System.currentTimeMillis(),
        onStartScan = onStartScan,
        onOpenProgram = onOpenProgram,
        onOpenHistory = onOpenHistory,
        onDismissPainWarning = viewModel::dismissPainWarning,
    )
}

@Composable
internal fun HomeContent(
    profile: UserProfile,
    state: HomeUiState,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    nowMs: Long = System.currentTimeMillis(),
    onStartScan: () -> Unit = {},
    onOpenProgram: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onDismissPainWarning: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TopBarHeader(
            profile = profile,
            greetingKind = HomeLogic.pickGreeting(
                latestRomTimestampMs = state.latestRom?.timestampMs,
                recentHighPainTimestampMs = null,    // pain log akan diisi Hari 12
                nowMs = nowMs,
            ),
        )

        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            SkeletonCard()
            Spacer(Modifier.height(12.dp))
            SkeletonCard()
            Spacer(Modifier.height(12.dp))
            SkeletonCard()
            return@Column
        }

        if (state.showPainWarning) {
            PainWarningBanner(
                onDismiss = onDismissPainWarning
            )
            Spacer(Modifier.height(16.dp))
        }

        // ─── Card 1: Scan ROM ──────────────────────────────────────
        ScanRomCard(
            scanCount = if (state.latestRom != null) 1 else 0,    // simplified MVP
            onClick = onStartScan,
        )

        Spacer(Modifier.height(12.dp))

        // ─── Card 2: Program Saya ──────────────────────────────────
        ProgramCard(
            program = state.activeProgram,
            hasAnyScan = state.latestRom != null,
            onClick = onOpenProgram,
        )

        Spacer(Modifier.height(12.dp))

        // ─── Card 3: Riwayat ROM Terbaru ──────────────────────────
        if (state.latestRom != null) {
            HistoryCard(
                rom = state.latestRom!!,
                relativeTime = HomeLogic.relativeTime(state.latestRom!!.timestampMs, nowMs),
                onClick = onOpenHistory,
            )
            Spacer(Modifier.height(12.dp))
        }

        // ─── Tips Card ─────────────────────────────────────────────
        TipsCard(
            tip = STATIC_TIPS[HomeLogic.pickTipIndexForToday(nowMs, STATIC_TIPS.size)],
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────
// Skeleton Loading
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SkeletonCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GemColors.Border.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {}
}

// ─────────────────────────────────────────────────────────────────
// Pain Warning Banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PainWarningBanner(onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GemColors.DangerBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = GemColors.Danger,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nyeri Anda cukup tinggi. Pertimbangkan konsultasi dokter sebelum sesi berikutnya.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GemColors.Danger)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tutup",
                    style = MaterialTheme.typography.labelMedium.copy(color = GemColors.Danger),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Top bar (kustom — bukan AppBar standard)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TopBarHeader(
    profile: UserProfile,
    greetingKind: GreetingKind,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Avatar(initials = profile.name.firstOrNull()?.toString().orEmpty())
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Halo, ${profile.name.split(" ").firstOrNull() ?: profile.name} 👋",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = greetingText(greetingKind),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun Avatar(initials: String) {
    Surface(
        shape = CircleShape,
        color = GemColors.EmeraldLight,
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = GemColors.EmeraldDark,
                    fontWeight = FontWeight.W600,
                ),
            )
        }
    }
}

private fun greetingText(kind: GreetingKind): String = when (kind) {
    GreetingKind.FirstTime -> "Selamat datang di Gemosto"
    GreetingKind.ReturningDefault -> "Bagaimana kabar lutut Anda hari ini?"
    GreetingKind.WeeklyCheckReminder -> "Sudah seminggu — saatnya cek ROM lagi?"
    GreetingKind.RecoverEncouragement -> "Semoga Anda merasa lebih baik hari ini."
}

// ─────────────────────────────────────────────────────────────────
// Card: Scan ROM (primary CTA)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ScanRomCard(
    scanCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GemColors.EmeraldPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scan ROM Lutut",
                    style = MaterialTheme.typography.titleLarge.copy(color = GemColors.OnPrimary),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ukur rentang gerak lutut Anda dalam 30 detik",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GemColors.OnPrimary.copy(alpha = 0.85f),
                    ),
                )
                if (scanCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Sudah pernah scan",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GemColors.OnPrimary.copy(alpha = 0.75f),
                        ),
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = GemColors.OnPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Card: Program Saya
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ProgramCard(
    program: ExerciseProgram?,
    hasAnyScan: Boolean,
    onClick: () -> Unit,
) {
    when {
        program == null && !hasAnyScan -> {
            // Belum bisa generate program — tampil placeholder disabled
            CardShell(borderColor = GemColors.Border) {
                Text(
                    text = "Program Saya",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Lakukan Scan ROM dulu untuk dapat rekomendasi latihan personal.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        program == null && hasAnyScan -> {
            CardShell(
                borderColor = GemColors.Border,
                onClick = onClick,
            ) {
                Text(
                    text = "Program Saya",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Anda belum punya program aktif. Tap untuk membuatnya.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        program?.level == ExerciseLevel.BLOCK || program?.status == ProgramStatus.BLOCKED_PAIN -> {
            CardShell(
                borderColor = GemColors.Danger,
                bgColor = GemColors.DangerBg,
                onClick = onClick,
            ) {
                Text(
                    text = "Konsultasi Dokter Disarankan",
                    style = MaterialTheme.typography.titleMedium.copy(color = GemColors.Danger),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = program.safetyNote ?: "Berdasarkan kondisi terbaru, " +
                        "kami sarankan konsultasi tenaga medis sebelum melanjutkan latihan.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        program != null -> {
            CardShell(borderColor = GemColors.Border, onClick = onClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Program Saya",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Chip(
                        label = program.level.displayLabel,
                        bgColor = GemColors.EmeraldLight,
                        textColor = GemColors.EmeraldDark,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Berdasarkan ROM Anda kategori ${program.romCategory.displayLabel}. " +
                        "${program.durationWeeks} minggu, ${program.frequencyPerWeek}.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = GemColors.EmeraldPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${program.exerciseCount} latihan",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Lihat program",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = GemColors.EmeraldPrimary,
                        ),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = GemColors.EmeraldPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Card: Riwayat ROM Terbaru
// ─────────────────────────────────────────────────────────────────

@Composable
private fun HistoryCard(
    rom: RomResult,
    relativeTime: String,
    onClick: () -> Unit,
) {
    CardShell(borderColor = GemColors.Border, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = GemColors.BackgroundSoft,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = GemColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scan Terakhir",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Text(
                    text = "Flexion ${rom.maxFlexionDeg.toInt()}° • ${rom.category.displayLabel}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = relativeTime,
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
// Card: Tips
// ─────────────────────────────────────────────────────────────────

private val STATIC_TIPS = listOf(
    "Konsistensi lebih penting dari intensitas. Lakukan latihan ringan tiap hari.",
    "Pemanasan 5 menit sebelum latihan dapat mengurangi risiko nyeri.",
    "Catat nyeri Anda — data ini membantu menyesuaikan latihan ke depannya.",
)

@Composable
private fun TipsCard(tip: String) {
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
                    text = "Tips Gemosto",
                    style = MaterialTheme.typography.labelMedium.copy(color = GemColors.EmeraldDark),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Reusable shell (card outline)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CardShell(
    borderColor: Color,
    bgColor: Color = GemColors.BackgroundDefault,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val mod = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
        .background(bgColor, shape = RoundedCornerShape(12.dp))
        .padding(16.dp)

    Column(modifier = mod) {
        content()
    }
}

@Composable
private fun Chip(
    label: String,
    bgColor: Color,
    textColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = textColor),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────

private val previewProfile = UserProfile(
    uid = "demo",
    name = "Kevin Banamtuan",
    email = "kbanamtuan10@gmail.com",
    photoUrl = null,
    age = 45,
    affectedKnee = KneeSide.RIGHT,
    activityLevel = com.gemosto.domain.model.ActivityLevel.MODERATE,
    disclaimerAcceptedAt = 1L,
    createdAt = 1L,
    updatedAt = 1L,
)

@Preview(showSystemUi = true, name = "First time")
@Composable
private fun HomeFirstTimePreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Preview tanpa ViewModel — show static state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBarHeader(profile = previewProfile, greetingKind = GreetingKind.FirstTime)
                Spacer(Modifier.height(16.dp))
                ScanRomCard(scanCount = 0, onClick = {})
                Spacer(Modifier.height(12.dp))
                ProgramCard(program = null, hasAnyScan = false, onClick = {})
                Spacer(Modifier.height(12.dp))
                TipsCard(tip = STATIC_TIPS[0])
            }
        }
    }
}

@Preview(showSystemUi = true, name = "Returning")
@Composable
private fun HomeReturningPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBarHeader(profile = previewProfile, greetingKind = GreetingKind.ReturningDefault)
                Spacer(Modifier.height(16.dp))
                ScanRomCard(scanCount = 3, onClick = {})
                Spacer(Modifier.height(12.dp))
                ProgramCard(
                    program = ExerciseProgram(
                        id = "p1", userId = "u",
                        generatedAt = System.currentTimeMillis(),
                        basedOnRomId = "r1",
                        romCategory = RomCategory.MILD,
                        level = ExerciseLevel.STRENGTHENING,
                        durationWeeks = 4,
                        frequencyPerWeek = "3-4x/minggu",
                        exercises = emptyList(),
                        narrative = null,
                        status = ProgramStatus.ACTIVE,
                    ),
                    hasAnyScan = true,
                    onClick = {},
                )
                Spacer(Modifier.height(12.dp))
                HistoryCard(
                    rom = RomResult(
                        id = "r1", userId = "u",
                        timestampMs = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L,
                        kneeSide = KneeSide.RIGHT,
                        maxFlexionDeg = 132f,
                        maxExtensionLagDeg = 4f,
                        category = RomCategory.NORMAL,
                        sessionDurationMs = 12000,
                    ),
                    relativeTime = "3 hari yang lalu",
                    onClick = {},
                )
                Spacer(Modifier.height(12.dp))
                TipsCard(tip = STATIC_TIPS[1])
            }
        }
    }
}
