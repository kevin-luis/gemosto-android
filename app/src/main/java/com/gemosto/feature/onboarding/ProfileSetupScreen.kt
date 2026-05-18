package com.gemosto.feature.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.R
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.KneeSide
import androidx.compose.foundation.text.KeyboardOptions
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onSignOut: () -> Unit = {},
    viewModel: ProfileSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveError) {
        val err = state.saveError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err, duration = SnackbarDuration.Long)
        viewModel.consumeError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.onboarding_profile_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
    ) { padding ->
        ProfileSetupContent(
            state = state,
            paddingValues = padding,
            onNameChange = viewModel::onNameChange,
            onAgeChange = viewModel::onAgeChange,
            onKneeSelected = viewModel::onKneeSelected,
            onActivitySelected = viewModel::onActivitySelected,
            onDisclaimerToggle = viewModel::onDisclaimerToggle,
            onSaveClick = viewModel::onSaveClick,
            onSignOut = onSignOut,
        )
    }
}

@Composable
internal fun ProfileSetupContent(
    state: ProfileSetupUiState,
    paddingValues: PaddingValues,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onKneeSelected: (KneeSide) -> Unit,
    onActivitySelected: (ActivityLevel) -> Unit,
    onDisclaimerToggle: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Intro
        Text(
            text = stringResource(R.string.onboarding_profile_intro),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Spacer(Modifier.height(16.dp))

        // ─── Field: Nama ───────────────────────────────────────
        SectionLabel(stringResource(R.string.onboarding_field_name))
        OutlinedTextField(
            value = state.nameInput,
            onValueChange = onNameChange,
            placeholder = { Text(stringResource(R.string.onboarding_field_name)) },
            singleLine = true,
            isError = state.nameInput.isNotBlank() && !state.isNameValid,
            supportingText = if (state.nameInput.isNotBlank() && !state.isNameValid) {
                { Text(stringResource(R.string.onboarding_name_error)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // ─── Field: Umur ───────────────────────────────────────
        SectionLabel(stringResource(R.string.onboarding_field_age))
        OutlinedTextField(
            value = state.ageInput,
            onValueChange = onAgeChange,
            placeholder = { Text("45") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.isAgeError,
            supportingText = {
                Text(
                    text = if (state.isAgeError) {
                        stringResource(R.string.onboarding_age_error_range)
                    } else {
                        stringResource(R.string.onboarding_field_age_helper)
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // ─── Section: Sisi Lutut ──────────────────────────────
        SectionLabel(stringResource(R.string.onboarding_section_knee))
        KneeSideRow(
            selected = state.affectedKnee,
            onSelect = onKneeSelected,
        )

        Spacer(Modifier.height(24.dp))

        // ─── Section: Level Aktivitas ──────────────────────────
        SectionLabel(stringResource(R.string.onboarding_section_activity))
        ActivityLevelOptions(
            selected = state.activityLevel,
            onSelect = onActivitySelected,
        )

        Spacer(Modifier.height(24.dp))

        // ─── Disclaimer Checkbox ───────────────────────────────
        DisclaimerCheckRow(
            checked = state.disclaimerAccepted,
            onToggle = onDisclaimerToggle,
        )

        Spacer(Modifier.height(32.dp))

        // ─── Tombol Selesai ────────────────────────────────────
        Button(
            onClick = onSaveClick,
            enabled = state.isFormValid && !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(R.string.onboarding_action_finish),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ─── Sign-out shortcut (testing) ──────────────────────
        // Akan di-hide di production; berguna saat development.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_action_signout),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .clickable(enabled = !state.isSaving) { onSignOut() }
                    .padding(12.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun KneeSideRow(
    selected: KneeSide?,
    onSelect: (KneeSide) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KneeSideChip(
            label = stringResource(R.string.onboarding_knee_left),
            isSelected = selected == KneeSide.LEFT,
            onClick = { onSelect(KneeSide.LEFT) },
            modifier = Modifier.weight(1f),
        )
        KneeSideChip(
            label = stringResource(R.string.onboarding_knee_right),
            isSelected = selected == KneeSide.RIGHT,
            onClick = { onSelect(KneeSide.RIGHT) },
            modifier = Modifier.weight(1f),
        )
        KneeSideChip(
            label = stringResource(R.string.onboarding_knee_both),
            isSelected = selected == KneeSide.BOTH,
            onClick = { onSelect(KneeSide.BOTH) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KneeSideChip(
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
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(color = textColor),
            )
        }
    }
}

@Composable
private fun ActivityLevelOptions(
    selected: ActivityLevel?,
    onSelect: (ActivityLevel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityOptionRow(
            level = ActivityLevel.LOW,
            title = stringResource(R.string.onboarding_activity_low),
            desc = stringResource(R.string.onboarding_activity_low_desc),
            selected = selected,
            onSelect = onSelect,
        )
        ActivityOptionRow(
            level = ActivityLevel.MODERATE,
            title = stringResource(R.string.onboarding_activity_moderate),
            desc = stringResource(R.string.onboarding_activity_moderate_desc),
            selected = selected,
            onSelect = onSelect,
        )
        ActivityOptionRow(
            level = ActivityLevel.ACTIVE,
            title = stringResource(R.string.onboarding_activity_active),
            desc = stringResource(R.string.onboarding_activity_active_desc),
            selected = selected,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ActivityOptionRow(
    level: ActivityLevel,
    title: String,
    desc: String,
    selected: ActivityLevel?,
    onSelect: (ActivityLevel) -> Unit,
) {
    val isSelected = selected == level
    val borderColor = if (isSelected) GemColors.EmeraldPrimary else GemColors.Border
    val bgColor = if (isSelected) GemColors.EmeraldLight else Color.Transparent

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(level) }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onSelect(level) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = GemColors.EmeraldPrimary,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DisclaimerCheckRow(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GemColors.BackgroundSoft,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = GemColors.EmeraldPrimary,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.onboarding_disclaimer_check),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun ProfileSetupContentPreview() {
    GemostoTheme {
        Scaffold { padding ->
            ProfileSetupContent(
                state = ProfileSetupUiState(
                    uid = "demo",
                    nameInput = "Kevin",
                    ageInput = "45",
                    affectedKnee = KneeSide.RIGHT,
                    activityLevel = ActivityLevel.MODERATE,
                    disclaimerAccepted = true,
                ),
                paddingValues = padding,
                onNameChange = {},
                onAgeChange = {},
                onKneeSelected = {},
                onActivitySelected = {},
                onDisclaimerToggle = {},
                onSaveClick = {},
                onSignOut = {},
            )
        }
    }
}
