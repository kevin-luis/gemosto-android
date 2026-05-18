package com.gemosto

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.exercise.ExerciseCatalog
import com.gemosto.domain.exercise.ExerciseId
import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseNarrative
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.NarrativeSource
import com.gemosto.domain.model.ProgramStatus
import com.gemosto.domain.model.RomCategory
import com.gemosto.domain.model.RomResult
import com.gemosto.domain.model.UserProfile
import com.gemosto.feature.account.AboutScreen
import com.gemosto.feature.account.AccountMainScreen
import com.gemosto.feature.account.DeleteAccountState
import com.gemosto.feature.account.DisclaimerScreen
import com.gemosto.feature.account.ProfileEditContent
import com.gemosto.feature.account.ProfileEditState
import com.gemosto.feature.exercise.DetailMode
import com.gemosto.feature.exercise.ExerciseDetailScreen
import com.gemosto.feature.exercise.PostExercisePainDialog
import com.gemosto.feature.exercise.ProgramEmptyState
import com.gemosto.feature.exercise.ProgramLoadingState
import com.gemosto.feature.exercise.ProgramScreen
import com.gemosto.feature.history.HistoryUiState
import com.gemosto.feature.history.RomHistoryContent
import com.gemosto.feature.home.HomeContent
import com.gemosto.feature.home.HomeUiState
import com.gemosto.feature.onboarding.ProfileSetupContent
import com.gemosto.feature.onboarding.ProfileSetupUiState
import com.gemosto.feature.onboarding.SplashScreen
import com.gemosto.feature.onboarding.WelcomeContent
import com.gemosto.feature.result.RomResultScreen
import com.gemosto.feature.scan.PoseQuality
import com.gemosto.feature.scan.RomCameraOverlayContent
import com.gemosto.feature.scan.ScanIntroScreen
import com.gemosto.feature.scan.ScanUiState
import com.gemosto.feature.scan.SessionPhase

@PreviewTest
@Preview(showSystemUi = true, name = "01 Splash")
@Composable
fun SplashScreenScreenshotPreview() {
    PreviewSurface {
        SplashScreen()
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "02 Welcome")
@Composable
fun WelcomeScreenScreenshotPreview() {
    PreviewSurface {
        WelcomeContent(
            isLoading = false,
            paddingValues = PaddingValues(0.dp),
            onSignInClick = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "03 Profile setup")
@Composable
fun ProfileSetupScreenScreenshotPreview() {
    PreviewSurface {
        ProfileSetupContent(
            state = ProfileSetupUiState(
                uid = "preview-user",
                email = "kevin@example.com",
                nameInput = "Kevin Banamtuan",
                ageInput = "45",
                affectedKnee = KneeSide.RIGHT,
                activityLevel = ActivityLevel.MODERATE,
                disclaimerAccepted = true,
            ),
            paddingValues = PaddingValues(0.dp),
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

@PreviewTest
@Preview(showSystemUi = true, name = "04 Home")
@Composable
fun HomeScreenScreenshotPreview() {
    PreviewSurface {
        HomeContent(
            profile = previewProfile,
            state = HomeUiState(
                isLoading = false,
                latestRom = previewRomResult(RomCategory.MILD),
                activeProgram = previewProgram(ExerciseLevel.STRENGTHENING, RomCategory.MILD),
                showPainWarning = true,
            ),
            paddingValues = PaddingValues(0.dp),
            nowMs = PREVIEW_NOW_MS,
            onStartScan = {},
            onOpenProgram = {},
            onOpenHistory = {},
            onDismissPainWarning = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "05 Scan intro")
@Composable
fun ScanIntroScreenScreenshotPreview() {
    PreviewSurface {
        ScanIntroScreen(
            paddingValues = PaddingValues(0.dp),
            selectedKneeSide = KneeSide.RIGHT,
            onKneeSelected = {},
            onReadyClick = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "06 ROM camera")
@Composable
fun RomCameraScreenScreenshotPreview() {
    PreviewSurface {
        RomCameraOverlayContent(
            state = ScanUiState(
                selectedKneeSide = KneeSide.RIGHT,
                currentAngleDeg = 142.4f,
                currentFlexionDeg = 37.6f,
                poseQuality = PoseQuality.Good,
                phase = SessionPhase.EXTENSION,
                phaseRemainingSeconds = 7,
            ),
            kneeSideLabel = "Kanan",
            paddingValues = PaddingValues(0.dp),
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "07 ROM result")
@Composable
fun RomResultScreenScreenshotPreview() {
    PreviewSurface {
        RomResultScreen(
            paddingValues = PaddingValues(0.dp),
            rom = previewRomResult(RomCategory.MODERATE),
            onScanAgain = {},
            onSeeRecommendation = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "08 Program")
@Composable
fun ProgramScreenScreenshotPreview() {
    PreviewSurface {
        ProgramScreen(
            paddingValues = PaddingValues(0.dp),
            program = previewProgram(ExerciseLevel.GENTLE, RomCategory.MODERATE),
            onStartSession = {},
            onExerciseClick = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "09 Program empty")
@Composable
fun ProgramEmptyScreenScreenshotPreview() {
    PreviewSurface {
        ProgramEmptyState(
            paddingValues = PaddingValues(0.dp),
            title = "Belum Ada Program",
            body = "Lakukan Scan ROM dulu untuk mendapatkan rekomendasi latihan personal.",
            primaryCtaLabel = "Mulai Scan ROM",
            onPrimaryCta = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "10 Program loading")
@Composable
fun ProgramLoadingScreenScreenshotPreview() {
    PreviewSurface {
        ProgramLoadingState(paddingValues = PaddingValues(0.dp))
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "11 Exercise detail")
@Composable
fun ExerciseDetailScreenScreenshotPreview() {
    PreviewSurface {
        ExerciseDetailScreen(
            paddingValues = PaddingValues(0.dp),
            exercise = ExerciseCatalog.get(ExerciseId.STRAIGHT_LEG_RAISE),
            mode = DetailMode.Session(currentIndex = 1, totalCount = 3),
            onAdvance = {},
            onStopForPain = {},
            onClose = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "12 Pain log")
@Composable
fun PostExercisePainDialogScreenshotPreview() {
    PreviewSurface {
        PostExercisePainDialog(
            onSave = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "13 History")
@Composable
fun RomHistoryScreenScreenshotPreview() {
    PreviewSurface {
        RomHistoryContent(
            state = HistoryUiState(
                isLoading = false,
                items = listOf(
                    previewRomResult(RomCategory.NORMAL, id = "rom-normal", daysAgo = 2),
                    previewRomResult(RomCategory.MILD, id = "rom-mild", daysAgo = 9),
                    previewRomResult(RomCategory.MODERATE, id = "rom-moderate", daysAgo = 16),
                ),
            ),
            onStartScan = {},
            onResultClick = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "14 Account")
@Composable
fun AccountScreenScreenshotPreview() {
    PreviewSurface {
        AccountMainScreen(
            profile = previewProfile,
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

@PreviewTest
@Preview(showSystemUi = true, name = "15 Profile edit")
@Composable
fun ProfileEditScreenScreenshotPreview() {
    PreviewSurface {
        ProfileEditContent(
            profile = previewProfile,
            state = ProfileEditState(
                isInitialized = true,
                name = previewProfile.name,
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

@PreviewTest
@Preview(showSystemUi = true, name = "16 About")
@Composable
fun AboutScreenScreenshotPreview() {
    PreviewSurface {
        AboutScreen(
            paddingValues = PaddingValues(0.dp),
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(showSystemUi = true, name = "17 Disclaimer")
@Composable
fun DisclaimerScreenScreenshotPreview() {
    PreviewSurface {
        DisclaimerScreen(
            paddingValues = PaddingValues(0.dp),
            onBack = {},
        )
    }
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

private fun previewRomResult(
    category: RomCategory,
    id: String = "rom-preview",
    daysAgo: Int = 3,
) = RomResult(
    id = id,
    userId = "preview-user",
    timestampMs = PREVIEW_NOW_MS - daysAgo * DAY_MS,
    kneeSide = KneeSide.RIGHT,
    maxFlexionDeg = when (category) {
        RomCategory.NORMAL -> 132f
        RomCategory.MILD -> 112f
        RomCategory.MODERATE -> 94f
        RomCategory.SEVERE -> 72f
    },
    maxExtensionLagDeg = when (category) {
        RomCategory.NORMAL -> 3f
        RomCategory.MILD -> 8f
        RomCategory.MODERATE -> 15f
        RomCategory.SEVERE -> 24f
    },
    category = category,
    sessionDurationMs = 12000L,
    deviceModel = "Preview",
    mediaPipeModel = "preview",
)

private fun previewProgram(
    level: ExerciseLevel,
    category: RomCategory,
) = ExerciseProgram(
    id = "program-preview",
    userId = "preview-user",
    generatedAt = PREVIEW_NOW_MS,
    basedOnRomId = "rom-preview",
    romCategory = category,
    level = level,
    durationWeeks = 4,
    frequencyPerWeek = "3x/minggu",
    exercises = listOf(
        ExerciseCatalog.get(ExerciseId.QUAD_SETS),
        ExerciseCatalog.get(ExerciseId.HEEL_SLIDES),
        ExerciseCatalog.get(ExerciseId.STRAIGHT_LEG_RAISE),
    ),
    narrative = ExerciseNarrative(
        intro = "Program ini disusun agar lutut Anda bergerak bertahap dan tetap aman.",
        rationale = "ROM Anda menunjukkan keterbatasan yang perlu dilatih perlahan. Latihan low-impact membantu menjaga gerak sendi tanpa dorongan berlebihan.",
        weeklyMotivation = "Mulai dari gerakan kecil yang konsisten.",
        source = NarrativeSource.FALLBACK_STATIC,
    ),
    status = ProgramStatus.ACTIVE,
)

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

private const val PREVIEW_NOW_MS = 1_765_152_000_000L
private const val DAY_MS = 24 * 60 * 60 * 1000L
