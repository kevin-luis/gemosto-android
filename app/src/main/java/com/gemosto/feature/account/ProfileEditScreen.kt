package com.gemosto.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    profile: UserProfile,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.initProfile(profile)
    }

    ProfileEditContent(
        profile = profile,
        state = state,
        paddingValues = paddingValues,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onAgeChange = viewModel::onAgeChange,
        onKneeSideSelect = viewModel::onKneeSideSelect,
        onActivityLevelSelect = viewModel::onActivityLevelSelect,
        onSaveClick = { viewModel.saveProfile(profile, onComplete = onBack) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileEditContent(
    profile: UserProfile,
    state: ProfileEditState,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onKneeSideSelect: (KneeSide) -> Unit,
    onActivityLevelSelect: (ActivityLevel) -> Unit,
    onSaveClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil", style = MaterialTheme.typography.titleLarge) },
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
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = profile.email,
                onValueChange = {},
                label = { Text("Email (tidak dapat diubah)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.age,
                onValueChange = onAgeChange,
                label = { Text("Umur (18-100 tahun)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Sisi Lutut Terdampak", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                KneeSideOption(
                    text = "Kiri",
                    selected = state.kneeSide == KneeSide.LEFT,
                    onClick = { onKneeSideSelect(KneeSide.LEFT) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                KneeSideOption(
                    text = "Kanan",
                    selected = state.kneeSide == KneeSide.RIGHT,
                    onClick = { onKneeSideSelect(KneeSide.RIGHT) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                KneeSideOption(
                    text = "Keduanya",
                    selected = state.kneeSide == KneeSide.BOTH,
                    onClick = { onKneeSideSelect(KneeSide.BOTH) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Tingkat Aktivitas", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ActivityLevelOption(
                title = "Rendah",
                subtitle = "Jarang olahraga, banyak duduk",
                selected = state.activityLevel == ActivityLevel.LOW,
                onClick = { onActivityLevelSelect(ActivityLevel.LOW) }
            )
            ActivityLevelOption(
                title = "Sedang",
                subtitle = "Jalan kaki / aktivitas ringan rutin",
                selected = state.activityLevel == ActivityLevel.MODERATE,
                onClick = { onActivityLevelSelect(ActivityLevel.MODERATE) }
            )
            ActivityLevelOption(
                title = "Aktif",
                subtitle = "Olahraga teratur tiap minggu",
                selected = state.activityLevel == ActivityLevel.ACTIVE,
                onClick = { onActivityLevelSelect(ActivityLevel.ACTIVE) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            androidx.compose.material3.Button(
                enabled = state.isValid && !state.isSaving,
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Simpan Perubahan", style = MaterialTheme.typography.labelLarge)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showSystemUi = true, name = "Profile edit")
@Composable
private fun ProfileEditScreenPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileEditContent(
                profile = previewProfile,
                state = ProfileEditState(
                    isInitialized = true,
                    name = "Kevin Banamtuan",
                    age = "45",
                    kneeSide = KneeSide.RIGHT,
                    activityLevel = ActivityLevel.MODERATE,
                    isValid = true,
                ),
                paddingValues = PaddingValues(0.dp),
                onBack = {},
                onNameChange = {},
                onAgeChange = {},
                onKneeSideSelect = {},
                onActivityLevelSelect = {},
                onSaveClick = {},
            )
        }
    }
}

private val previewProfile = UserProfile(
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
private fun KneeSideOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.selectable(selected = selected, onClick = onClick)
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}

@Composable
private fun ActivityLevelOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GemColors.TextSecondary)
        }
    }
}
