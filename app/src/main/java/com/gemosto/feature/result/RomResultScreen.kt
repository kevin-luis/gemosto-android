package com.gemosto.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.RomCategory
import com.gemosto.domain.model.RomResult

/**
 * Layar hasil scan ROM.
 *
 * Spec: 002-rom-scan.md section 4 — RomResultScreen.
 *
 * Komponen:
 *  - Severity chip besar (Normal/Mild/Moderate/Severe)
 *  - 2 metric: Flexion + Extension lag
 *  - Card "Apa artinya?" dengan penjelasan singkat per kategori
 *  - Disclaimer banner permanen
 *  - CTA: "Lihat Rekomendasi Latihan" (primary) + "Scan Ulang" (secondary)
 */
@Composable
fun RomResultScreen(
    paddingValues: PaddingValues,
    rom: RomResult,
    onScanAgain: () -> Unit,
    onSeeRecommendation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Hasil Scan ROM",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        ResultMainCard(rom = rom)

        Spacer(Modifier.height(12.dp))

        ExplanationCard(category = rom.category)

        Spacer(Modifier.height(12.dp))

        DisclaimerBanner()

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSeeRecommendation,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Lihat Rekomendasi Latihan", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onScanAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Scan Ulang", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────
// Main result card — severity chip + 2 metric
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ResultMainCard(rom: RomResult) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GemColors.Border, RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Lutut ${kneeSideLabel(rom.kneeSide)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(8.dp))

            SeverityChipLarge(category = rom.category)

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricColumn(
                    value = "${rom.maxFlexionDeg.toInt()}°",
                    label = "Tekukan max\n(Flexion)",
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(56.dp)
                        .background(GemColors.Border),
                )
                MetricColumn(
                    value = "${rom.maxExtensionLagDeg.toInt()}°",
                    label = "Sisa lurus\n(Extension lag)",
                )
            }
        }
    }
}

@Composable
private fun SeverityChipLarge(category: RomCategory) {
    val (bg, fg) = when (category) {
        RomCategory.NORMAL -> GemColors.EmeraldLight to GemColors.EmeraldDark
        RomCategory.MILD -> Color(0xFFFEF3C7) to Color(0xFF92400E)        // amber light/dark
        RomCategory.MODERATE -> Color(0xFFFFEDD5) to Color(0xFF9A3412)    // orange light/dark
        RomCategory.SEVERE -> GemColors.DangerBg to GemColors.Danger
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(fg, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Kategori: ${category.displayLabel}",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = fg,
                    fontWeight = FontWeight.W600,
                ),
            )
        }
    }
}

@Composable
private fun MetricColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = GemColors.EmeraldDark,
                fontWeight = FontWeight.W600,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Explanation card per kategori
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ExplanationCard(category: RomCategory) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GemColors.BackgroundSoft,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GemColors.Border, RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Apa artinya?",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = explanationText(category),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun explanationText(category: RomCategory): String = when (category) {
    RomCategory.NORMAL -> "Rentang gerak lutut Anda dalam batas normal. " +
        "Pertahankan dengan latihan rutin untuk menjaga kekuatan dan fleksibilitas sendi."
    RomCategory.MILD -> "Terdapat keterbatasan ringan pada rentang gerak lutut Anda. " +
        "Latihan penguatan terkontrol dapat membantu memperbaiki kondisi ini."
    RomCategory.MODERATE -> "Keterbatasan rentang gerak Anda berada di kategori sedang. " +
        "Mulai dengan latihan low-impact dan pantau respon nyeri secara seksama."
    RomCategory.SEVERE -> "Rentang gerak Anda sangat terbatas. Kami sarankan konsultasi dengan " +
        "dokter atau fisioterapis sebelum memulai program latihan rumahan."
}

// ─────────────────────────────────────────────────────────────────
// Disclaimer banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun DisclaimerBanner() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GemColors.EmeraldLight,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = GemColors.EmeraldDark,
                modifier = Modifier.size(16.dp).padding(top = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Hasil ini berbasis estimasi pose dari kamera, bukan pengukuran goniometer " +
                    "medis. Bukan pengganti diagnosis dokter — konsultasi tenaga medis untuk " +
                    "evaluasi lebih lanjut.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = GemColors.EmeraldDark,
                ),
            )
        }
    }
}

private fun kneeSideLabel(side: KneeSide): String = when (side) {
    KneeSide.LEFT -> "Kiri"
    KneeSide.RIGHT -> "Kanan"
    KneeSide.BOTH -> "—"
}

// ─────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────

@Preview(showSystemUi = true, name = "Normal")
@Composable
private fun ResultNormalPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RomResultScreen(
                paddingValues = PaddingValues(0.dp),
                rom = sampleRom(132f, 4f, RomCategory.NORMAL),
                onScanAgain = {},
                onSeeRecommendation = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Moderate")
@Composable
private fun ResultModeratePreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RomResultScreen(
                paddingValues = PaddingValues(0.dp),
                rom = sampleRom(98f, 14f, RomCategory.MODERATE),
                onScanAgain = {},
                onSeeRecommendation = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Severe")
@Composable
private fun ResultSeverePreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RomResultScreen(
                paddingValues = PaddingValues(0.dp),
                rom = sampleRom(75f, 22f, RomCategory.SEVERE),
                onScanAgain = {},
                onSeeRecommendation = {},
            )
        }
    }
}

private fun sampleRom(flex: Float, lag: Float, cat: RomCategory) = RomResult(
    id = "preview",
    userId = "u",
    timestampMs = 0L,
    kneeSide = KneeSide.RIGHT,
    maxFlexionDeg = flex,
    maxExtensionLagDeg = lag,
    category = cat,
    sessionDurationMs = 12000L,
    deviceModel = "Preview",
    mediaPipeModel = "preview",
)
