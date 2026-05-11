package com.gemosto.feature.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.RomCategory
import com.gemosto.domain.model.RomResult
import com.gemosto.feature.home.HomeLogic
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomHistoryScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: HistoryViewModel = koinViewModel(),
    onStartScan: () -> Unit = {},
    onResultClick: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat ROM", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) }
            )
        },
        modifier = Modifier.padding(paddingValues)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DisclaimerBanner()
            
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GemColors.EmeraldPrimary)
                }
            } else if (state.items.isEmpty()) {
                EmptyHistoryState(onStartScan = onStartScan)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items) { result ->
                        HistoryCard(rom = result, onClick = { onResultClick(result.id) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = GemColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Belum ada scan",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Lakukan scan ROM pertama Anda untuk melihat riwayat di sini",
            style = MaterialTheme.typography.bodyMedium,
            color = GemColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Mulai Scan ROM", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DisclaimerBanner() {
    Surface(
        shape = RoundedCornerShape(0.dp), // or 8.dp if padded
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
                text = "Hasil scan ini adalah perkiraan dari deteksi pose kamera, " +
                    "bukan alat diagnostik medis.",
                style = MaterialTheme.typography.bodySmall.copy(color = GemColors.EmeraldDark),
            )
        }
    }
}

@Composable
private fun HistoryCard(rom: RomResult, onClick: () -> Unit) {
    val nowMs = System.currentTimeMillis()
    val relativeTimeStr = HomeLogic.relativeTime(rom.timestampMs, nowMs)
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
    val dateStr = dateFormat.format(Date(rom.timestampMs))

    val severityColor = when (rom.category) {
        RomCategory.NORMAL -> GemColors.EmeraldPrimary
        RomCategory.MILD -> GemColors.Warning
        RomCategory.MODERATE -> GemColors.Warning
        RomCategory.SEVERE -> GemColors.Danger
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, GemColors.Border, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = severityColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = rom.category.displayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = severityColor, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (rom.kneeSide == KneeSide.LEFT) "Lutut Kiri" else "Lutut Kanan",
                    style = MaterialTheme.typography.labelMedium,
                    color = GemColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Flexion",
                        style = MaterialTheme.typography.labelSmall,
                        color = GemColors.TextSecondary
                    )
                    Text(
                        text = "${rom.maxFlexionDeg.toInt()}°",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Extension Lag",
                        style = MaterialTheme.typography.labelSmall,
                        color = GemColors.TextSecondary
                    )
                    Text(
                        text = "${rom.maxExtensionLagDeg.toInt()}°",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$dateStr • $relativeTimeStr",
                style = MaterialTheme.typography.bodySmall,
                color = GemColors.TextSecondary
            )
        }
    }
}
