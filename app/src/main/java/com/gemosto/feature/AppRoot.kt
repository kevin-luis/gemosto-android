package com.gemosto.feature

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.prefs.UserPrefs
import com.gemosto.domain.model.AuthState
import com.gemosto.domain.model.ProfileState
import com.gemosto.feature.onboarding.ProfileSetupScreen
import com.gemosto.feature.onboarding.SplashScreen
import com.gemosto.feature.onboarding.WelcomeScreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Root composable — install NavHost dan reactive routing berdasar
 * [AuthState] + [ProfileState] dari [AppViewModel].
 *
 * Routing rules (sesuai spec 001 section 6):
 *   - Loading              → SPLASH
 *   - SignedOut            → WELCOME
 *   - SignedIn + Missing   → PROFILE_SETUP
 *   - SignedIn + Loaded    → HOME
 */
@Composable
fun AppRoot() {
    val appViewModel: AppViewModel = koinViewModel()
    val authState by appViewModel.authState.collectAsStateWithLifecycle()
    val profileState by appViewModel.profileState.collectAsStateWithLifecycle()
    val authRepo: AuthRepository = koinInject()
    val userPrefs: UserPrefs = koinInject()

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Reactive routing: kalau state berubah → navigate ke route yang tepat.
    LaunchedEffect(authState, profileState) {
        val target = computeTargetRoute(authState, profileState)
        if (target != null) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val onSignOut: () -> Unit = {
        scope.launch {
            // Clear local prefs dulu (mis. onboarding flag) — next sign-in
            // dengan akun lain harus mulai bersih.
            userPrefs.clearAll()
            authRepo.signOut()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen()
        }
        composable(Routes.WELCOME) {
            WelcomeScreen()
        }
        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(onSignOut = onSignOut)
        }
        composable(Routes.HOME) {
            // ProfileState pasti Loaded di sini berdasarkan routing,
            // tapi kita guard untuk safety race condition.
            val loaded = profileState as? ProfileState.Loaded ?: return@composable
            MainScaffold(profile = loaded.profile, onSignOut = onSignOut)
        }
    }
}

private fun computeTargetRoute(auth: AuthState, profile: ProfileState): String? = when (auth) {
    AuthState.Loading -> Routes.SPLASH
    AuthState.SignedOut -> Routes.WELCOME
    is AuthState.SignedIn -> when (profile) {
        ProfileState.Loading -> Routes.SPLASH
        ProfileState.Missing -> Routes.PROFILE_SETUP
        is ProfileState.Loaded -> Routes.HOME
    }
}
