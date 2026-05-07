package com.gemosto.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme

/**
 * Layar splash sederhana — tampil saat AuthState/ProfileState masih Loading.
 *
 * Background emerald solid, logo G stylized di tengah, brand name "Gemosto"
 * dan tagline. Auto-transition di-handle di AppRoot via routing.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GemColors.EmeraldPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Placeholder logo "G" — circular outline + tail (stylistic letter G)
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.W600,
                        color = GemColors.OnPrimary,
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Gemosto",
                style = MaterialTheme.typography.headlineLarge.copy(color = GemColors.OnPrimary),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pendamping kesehatan lutut Anda",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = GemColors.OnPrimary.copy(alpha = 0.85f),
                ),
            )
        }
    }
}

@Preview(showBackground = false, showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    GemostoTheme { SplashScreen() }
}
