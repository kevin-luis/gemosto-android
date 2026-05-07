package com.gemosto.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.firestore.ProfileRepository
import com.gemosto.domain.model.AuthState
import com.gemosto.domain.model.ProfileState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Top-level ViewModel — pegang AuthState + ProfileState yang dipakai
 * untuk routing di [AppRoot].
 *
 * Penjelasan flow:
 * - `authState`: Flow real-time dari Firebase Auth listener
 * - `profileState`: turunan dari authState — kalau SignedIn, observe Firestore
 *   profile doc; kalau SignedOut, langsung Loading (akan ditimpa setelah login)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepo.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Loading,
        )

    val profileState: StateFlow<ProfileState> = authState
        .flatMapLatest { auth ->
            when (auth) {
                AuthState.Loading -> flowOf(ProfileState.Loading)
                AuthState.SignedOut -> flowOf(ProfileState.Loading)
                is AuthState.SignedIn -> profileRepo.observe(auth.uid)
                    .map { profile ->
                        if (profile == null) {
                            ProfileState.Missing
                        } else {
                            ProfileState.Loaded(profile)
                        }
                    }
                    .catch { e ->
                        // Catch Firestore exceptions (like PERMISSION_DENIED)
                        // to prevent crashing the app.
                        // Emit Missing so the user can proceed to setup, where writes
                        // will also gracefully fail (via runCatching) and show an error message
                        // if the Firestore rules are still not configured properly.
                        e.printStackTrace()
                        emit(ProfileState.Missing)
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ProfileState.Loading,
        )
}
