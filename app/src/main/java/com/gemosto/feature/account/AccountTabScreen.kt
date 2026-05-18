package com.gemosto.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

enum class AccountRoute {
    MAIN, EDIT_PROFILE, ABOUT, DISCLAIMER
}

@Composable
fun AccountTabScreen(
    profile: UserProfile,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onSignOut: () -> Unit = {},
    viewModel: AccountViewModel = koinViewModel()
) {
    var currentRoute by remember { mutableStateOf(AccountRoute.MAIN) }
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()

    BackHandler(enabled = currentRoute != AccountRoute.MAIN) {
        currentRoute = AccountRoute.MAIN
    }

    when (currentRoute) {
        AccountRoute.MAIN -> AccountMainScreen(
            profile = profile,
            paddingValues = paddingValues,
            onNavigate = { currentRoute = it },
            onSignOut = onSignOut,
            deleteState = deleteState,
            onSignOutClick = viewModel::signOut,
            onDeleteAccount = viewModel::deleteAccount,
            onConsumeDeleteState = viewModel::consumeDeleteState,
        )
        AccountRoute.EDIT_PROFILE -> ProfileEditScreen(
            profile = profile,
            paddingValues = paddingValues,
            onBack = { currentRoute = AccountRoute.MAIN }
        )
        AccountRoute.ABOUT -> AboutScreen(onBack = { currentRoute = AccountRoute.MAIN }, paddingValues = paddingValues)
        AccountRoute.DISCLAIMER -> DisclaimerScreen(onBack = { currentRoute = AccountRoute.MAIN }, paddingValues = paddingValues)
    }
}

@Composable
internal fun AccountMainScreen(
    profile: UserProfile,
    paddingValues: PaddingValues,
    onNavigate: (AccountRoute) -> Unit,
    onSignOut: () -> Unit,
    deleteState: DeleteAccountState,
    onSignOutClick: (() -> Unit) -> Unit,
    onDeleteAccount: () -> Unit,
    onConsumeDeleteState: () -> Unit,
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteStep1 by remember { mutableStateOf(false) }
    var showDeleteStep2 by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }

    LaunchedEffect(deleteState) {
        if (deleteState is DeleteAccountState.Success) {
            onConsumeDeleteState()
            onSignOut() // Using onSignOut to clear local user state and redirect to welcome
        } else if (deleteState is DeleteAccountState.NeedsReAuth) {
            // Handle re-auth if needed
            onConsumeDeleteState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Akun",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Section 1: Profile Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = GemColors.EmeraldLight,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profile.name.firstOrNull()?.toString().orEmpty().uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = GemColors.EmeraldDark,
                            fontWeight = FontWeight.W600
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemColors.TextSecondary
                )
            }
            TextButton(
                onClick = { onNavigate(AccountRoute.EDIT_PROFILE) }
            ) {
                Text("Edit Profil", color = GemColors.EmeraldPrimary)
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, color = GemColors.Border)

        // Section 2: Menu List
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            AccountMenuItem(
                icon = Icons.Outlined.Edit,
                label = "Edit Profil",
                onClick = { onNavigate(AccountRoute.EDIT_PROFILE) }
            )
            AccountMenuItem(
                icon = Icons.Outlined.Info,
                label = "Tentang Aplikasi",
                onClick = { onNavigate(AccountRoute.ABOUT) }
            )
            AccountMenuItem(
                icon = Icons.Outlined.Shield,
                label = "Disclaimer Medis",
                onClick = { onNavigate(AccountRoute.DISCLAIMER) }
            )
            AccountMenuItem(
                icon = Icons.Outlined.Logout,
                label = "Keluar",
                onClick = { showSignOutDialog = true }
            )
        }

        Divider(color = GemColors.Border)

        // Section 3: Danger Zone
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { showDeleteStep1 = true }
            ) {
                Text("Hapus Akun", color = GemColors.Danger)
            }
            Text(
                text = "Tindakan ini tidak dapat dibatalkan",
                style = MaterialTheme.typography.bodySmall,
                color = GemColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Section 4: Footer
        Text(
            text = "Gemosto v0.1.0 (MVP) — 2026",
            style = MaterialTheme.typography.labelSmall,
            color = GemColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )
    }

    // Dialogs
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Yakin keluar?") },
            text = { Text("Anda harus masuk kembali untuk melihat program latihan Anda.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onSignOutClick(onSignOut)
                }) {
                    Text("Ya, Keluar", color = GemColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Batal", color = GemColors.TextPrimary)
                }
            }
        )
    }

    if (showDeleteStep1) {
        AlertDialog(
            onDismissRequest = { showDeleteStep1 = false },
            title = { Text("Hapus Akun Permanen") },
            text = { Text("Tindakan ini tidak dapat dibatalkan. Semua data Anda akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteStep1 = false
                    showDeleteStep2 = true
                }) {
                    Text("Lanjutkan", color = GemColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep1 = false }) {
                    Text("Batal", color = GemColors.TextPrimary)
                }
            }
        )
    }

    if (showDeleteStep2) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteStep2 = false 
                deleteConfirmationText = ""
            },
            title = { Text("Konfirmasi Hapus") },
            text = {
                Column {
                    Text("Ketik 'HAPUS' untuk konfirmasi")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = { deleteConfirmationText = it },
                        singleLine = true,
                        placeholder = { Text("HAPUS") }
                    )
                    if (deleteState is DeleteAccountState.Error) {
                        Text(
                            text = (deleteState as DeleteAccountState.Error).message,
                            color = GemColors.Danger,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteAccount,
                    enabled = deleteConfirmationText == "HAPUS" && deleteState !is DeleteAccountState.Loading
                ) {
                    if (deleteState is DeleteAccountState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Hapus Akun", color = GemColors.Danger)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteStep2 = false
                    deleteConfirmationText = ""
                    onConsumeDeleteState()
                }) {
                    Text("Batal", color = GemColors.TextPrimary)
                }
            }
        )
    }
}

@Preview(showSystemUi = true, name = "Account")
@Composable
private fun AccountMainScreenPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AccountMainScreen(
                profile = previewAccountProfile,
                paddingValues = PaddingValues(0.dp),
                onNavigate = {},
                onSignOut = {},
                deleteState = DeleteAccountState.Idle,
                onSignOutClick = { it() },
                onDeleteAccount = {},
                onConsumeDeleteState = {},
            )
        }
    }
}

private val previewAccountProfile = UserProfile(
    uid = "preview-user",
    name = "Kevin Banamtuan",
    email = "kevin@example.com",
    photoUrl = null,
    age = 45,
    affectedKnee = KneeSide.RIGHT,
    activityLevel = ActivityLevel.MODERATE,
    disclaimerAcceptedAt = 1L,
    createdAt = 1L,
    updatedAt = 1L,
)

@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GemColors.EmeraldPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = GemColors.TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}
