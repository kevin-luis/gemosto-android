package com.gemosto.feature.account

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gemosto.core.designsystem.GemColors
import com.gemosto.domain.model.UserProfile

/**
 * Placeholder Hari 13 — diisi penuh saat spec 008 (Account & Settings).
 * Hari 4: tampil profil + tombol Sign Out (testing).
 */
@Composable
fun AccountTabScreen(
    profile: UserProfile,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onSignOut: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
    ) {
        Text(
            text = "Akun",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = GemColors.EmeraldLight,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profile.name.firstOrNull()?.toString().orEmpty().uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = GemColors.EmeraldDark,
                            fontWeight = FontWeight.W600,
                        ),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    profile.email,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Edit profil, Tentang Aplikasi, Disclaimer Medis, dan Hapus Akun " +
                "akan diimplementasi Hari 13.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GemColors.Danger,
                contentColor = GemColors.OnPrimary,
            ),
        ) {
            Text("Keluar", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))
    }
}
