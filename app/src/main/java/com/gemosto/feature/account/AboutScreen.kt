package com.gemosto.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gemosto.core.designsystem.GemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tentang Aplikasi", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        modifier = Modifier.padding(paddingValues)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Placeholder for logo
            Text(
                text = "Gemosto",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GemColors.EmeraldPrimary
                )
            )
            
            Text(
                text = "v0.1.0 (MVP)",
                style = MaterialTheme.typography.labelMedium,
                color = GemColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Pendamping kesehatan lutut Anda — ukur ROM, dapatkan latihan terpersonalisasi.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tentang Nama",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"Gem\" merujuk pada Generative AI yang menjadi pendukung narasi dan motivasi Anda. \"Osto\" merujuk pada osteoartritis, fokus medis aplikasi ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Skripsi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kevin Banamtuan — Universitas Kristen Duta Wacana — 2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Teknologi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Kotlin + Jetpack Compose\n" +
                           "• Google MediaPipe untuk deteksi pose on-device\n" +
                           "• Firebase untuk autentikasi & data\n" +
                           "• Google Gemini untuk narasi latihan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemColors.TextSecondary
                )
            }
        }
    }
}
