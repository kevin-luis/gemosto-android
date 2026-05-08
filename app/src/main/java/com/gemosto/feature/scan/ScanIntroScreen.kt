package com.gemosto.feature.scan

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
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.R
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.model.KneeSide

/**
 * Layar persiapan sebelum mulai scan ROM.
 *
 * Spec: 002-rom-scan.md section 4 — ScanIntroScreen.
 *
 * Berisi:
 *  - Hero illustration (placeholder icon)
 *  - Headline + subheadline
 *  - Checklist 4 item persiapan
 *  - Pilihan sisi lutut yang akan diukur (chip Kiri / Kanan)
 *  - Footer: tombol "Saya siap, mulai scan"
 */
@Composable
fun ScanIntroScreen(
    paddingValues: PaddingValues,
    selectedKneeSide: KneeSide?,
    onKneeSelected: (KneeSide) -> Unit,
    onReadyClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // App bar custom
        Text(
            text = stringResource(R.string.scan_intro_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        // Hero ilustrasi placeholder
        HeroIllustration()

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.scan_intro_section_prep),
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.scan_intro_section_prep_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        Spacer(Modifier.height(16.dp))

        // Checklist 4 item
        ChecklistItem(text = stringResource(R.string.scan_intro_check_seat))
        ChecklistItem(text = stringResource(R.string.scan_intro_check_distance))
        ChecklistItem(text = stringResource(R.string.scan_intro_check_visible))
        ChecklistItem(text = stringResource(R.string.scan_intro_check_lighting))

        Spacer(Modifier.height(24.dp))

        // Pilihan sisi lutut
        Text(
            text = stringResource(R.string.scan_intro_section_knee_to_measure),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        KneeSidePicker(
            selected = selectedKneeSide,
            onSelect = onKneeSelected,
        )

        Spacer(Modifier.height(32.dp))

        // CTA "Saya siap"
        Button(
            onClick = onReadyClick,
            enabled = selectedKneeSide != null && selectedKneeSide != KneeSide.BOTH,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.scan_intro_action_ready),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Disclaimer
        Text(
            text = stringResource(R.string.disclaimer_short),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroIllustration() {
    // Placeholder hero — akan diganti dengan SVG ilustrasi posisi duduk + kamera
    // saat polish hari 14.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GemColors.EmeraldLight),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccessibilityNew,
            contentDescription = null,
            tint = GemColors.EmeraldPrimary,
            modifier = Modifier.size(80.dp),
        )
    }
}

@Composable
private fun ChecklistItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = GemColors.EmeraldPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KneeSidePicker(
    selected: KneeSide?,
    onSelect: (KneeSide) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KneeChip(
            label = stringResource(R.string.onboarding_knee_left),
            isSelected = selected == KneeSide.LEFT,
            onClick = { onSelect(KneeSide.LEFT) },
            modifier = Modifier.weight(1f),
        )
        KneeChip(
            label = stringResource(R.string.onboarding_knee_right),
            isSelected = selected == KneeSide.RIGHT,
            onClick = { onSelect(KneeSide.RIGHT) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KneeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) GemColors.EmeraldPrimary else Color.Transparent
    val textColor = if (isSelected) GemColors.OnPrimary else GemColors.TextPrimary
    val borderColor = if (isSelected) GemColors.EmeraldPrimary else GemColors.Border

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(color = textColor),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ScanIntroPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScanIntroScreen(
                paddingValues = PaddingValues(0.dp),
                selectedKneeSide = KneeSide.RIGHT,
                onKneeSelected = {},
                onReadyClick = {},
            )
        }
    }
}
