package com.gemosto.core.di

import com.gemosto.R
import com.gemosto.core.common.DefaultDispatcherProvider
import com.gemosto.core.common.DispatcherProvider
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.auth.FirebaseAuthRepository
import com.gemosto.data.auth.GoogleSignInClient
import com.gemosto.data.firestore.ExerciseRepository
import com.gemosto.data.firestore.FirestoreExerciseRepository
import com.gemosto.data.firestore.FirestoreProfileRepository
import com.gemosto.data.firestore.FirestoreRomRepository
import com.gemosto.data.firestore.PainLogRepository
import com.gemosto.data.firestore.FirestorePainLogRepository
import com.gemosto.data.firestore.ProfileRepository
import com.gemosto.data.firestore.RomRepository
import com.gemosto.data.llm.GeminiGemoService
import com.gemosto.data.llm.GeminiNarrativeService
import com.gemosto.data.llm.GemoAiRepository
import com.gemosto.data.llm.GemoAiRepositoryImpl
import com.gemosto.data.pose.PoseDetector
import com.gemosto.data.prefs.UserPrefs
import com.gemosto.domain.exercise.ExerciseRuleEngine
import com.gemosto.domain.gemo.SuggestedQuestion
import com.gemosto.feature.account.AccountViewModel
import com.gemosto.feature.account.ProfileEditViewModel
import com.gemosto.feature.AppViewModel
import com.gemosto.feature.exercise.ProgramViewModel
import com.gemosto.feature.gemo.GemoChatViewModel
import com.gemosto.feature.history.HistoryViewModel
import com.gemosto.feature.home.HomeViewModel
import com.gemosto.feature.onboarding.ProfileSetupViewModel
import com.gemosto.feature.onboarding.WelcomeViewModel
import com.gemosto.feature.scan.ScanViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Single Koin module untuk seluruh app.
 *
 * Filosofi: untuk MVP 1 module project, kita TIDAK split DI per-feature
 * supaya simple. Saat module bertambah > ~30 binding, baru dipertimbangkan split.
 *
 * Struktur urutan: core → data → domain (rule engines) → feature ViewModels.
 */
val appModule = module {

    // ─── Core ────────────────────────────────────────────────
    single<DispatcherProvider> { DefaultDispatcherProvider() }

    // ─── Firebase singletons ─────────────────────────────────
    single<FirebaseAuth> { Firebase.auth }
    single<FirebaseFirestore> { Firebase.firestore }

    // ─── Data layer ──────────────────────────────────────────
    single<AuthRepository> { FirebaseAuthRepository(get()) }
    single<ProfileRepository> { FirestoreProfileRepository(get()) }
    single<RomRepository> { FirestoreRomRepository(get()) }
    single<ExerciseRepository> { FirestoreExerciseRepository(get()) }
    single<PainLogRepository> { FirestorePainLogRepository(get(), get()) }
    single { UserPrefs(androidContext()) }
    single { PoseDetector() }
    single { GeminiNarrativeService() }
    single { GeminiGemoService() }
    single<GemoAiRepository> { GemoAiRepositoryImpl(get()) }

    // ─── Domain (pure Kotlin) ────────────────────────────────
    factory { ExerciseRuleEngine() }

    // GoogleSignInClient — webClientId di-resolve dari resource
    // yang auto-generated oleh google-services plugin dari google-services.json.
    single {
        GoogleSignInClient(
            webClientId = androidContext().getString(R.string.default_web_client_id)
        )
    }

    // ─── Domain (pure Kotlin, no Android deps) ───────────────
    // factory { ExerciseRuleEngine() }   // akan di-add di Hari 9-10
    // (RomAngleCalculator adalah `object` — tidak perlu DI)

    // ─── Feature ViewModels ──────────────────────────────────
    viewModel { AppViewModel(get(), get()) }
    viewModel { WelcomeViewModel(get(), get()) }
    viewModel { ProfileSetupViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { ScanViewModel(get(), get(), get()) }
    viewModel { 
        ProgramViewModel(
            authRepo = get(),
            profileRepo = get(),
            romRepo = get(),
            exerciseRepo = get(),
            painLogRepo = get(),
            ruleEngine = get(),
            narrativeService = get()
        ) 
    }

    // viewModel { HomeViewModel(get(), get(), get(), get()) }  // Hari 4
    // viewModel { RomCameraViewModel(get(), get(), get()) }    // Hari 5-7
    // viewModel { ProgramViewModel(get(), get(), get(), get()) } // Hari 9-11
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { AccountViewModel(get(), get(), get(), get()) }
    viewModel { ProfileEditViewModel(get()) }
    viewModel {
        GemoChatViewModel(
            gemoAiRepository = get(),
            initialSuggestedQuestions = listOf(
                SuggestedQuestion(
                    text = androidContext().getString(R.string.gemo_suggested_question_oa_basics),
                ),
                SuggestedQuestion(
                    text = androidContext().getString(R.string.gemo_suggested_question_exercise),
                ),
                SuggestedQuestion(
                    text = androidContext().getString(R.string.gemo_suggested_question_stiffness),
                ),
                SuggestedQuestion(
                    text = androidContext().getString(R.string.gemo_suggested_question_doctor),
                ),
            ),
        )
    }
}
