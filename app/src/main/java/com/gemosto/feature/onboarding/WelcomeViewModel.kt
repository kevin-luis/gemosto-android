package com.gemosto.feature.onboarding

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.auth.GoogleSignInClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk WelcomeScreen.
 *
 * State machine:
 * - Idle: tombol enabled, tidak ada error
 * - Loading: spinner pengganti tombol
 * - Error: snackbar message + tombol kembali enabled
 *
 * Setelah signIn sukses, AuthState di [com.gemosto.feature.AppViewModel]
 * akan update ke SignedIn → AppRoot routing redirect ke ProfileSetup atau Home.
 */
class WelcomeViewModel(
    private val authRepo: AuthRepository,
    private val googleSignIn: GoogleSignInClient,
) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun onSignInClick(activityContext: Context) {
        if (_state.value.isLoading) return
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = googleSignIn.signIn(activityContext)) {
                is GoogleSignInClient.Result.Success -> {
                    val authResult = authRepo.signInWithGoogle(result.idToken)
                    if (authResult.isSuccess) {
                        // Berhasil — AuthState listener akan trigger routing.
                        // State Loading kita biarkan true agar tombol stay disabled
                        // sampai screen di-recompose ke route lain.
                        _state.value = _state.value.copy(isLoading = false)
                    } else {
                        val cause = authResult.exceptionOrNull()
                        Log.e(TAG, "Firebase signInWithGoogle failed", cause)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = WelcomeError(
                                kind = WelcomeErrorKind.AuthFailed,
                                message = "Verifikasi Firebase gagal: " +
                                          "${cause?.javaClass?.simpleName} — ${cause?.message ?: "—"}",
                            ),
                        )
                    }
                }
                GoogleSignInClient.Result.Canceled -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = WelcomeError(
                            kind = WelcomeErrorKind.Canceled,
                            message = "Login dibatalkan.",
                        ),
                    )
                }
                is GoogleSignInClient.Result.Failed -> {
                    Log.e(TAG, "Google sign-in failed: type=${result.errorType}", result.cause)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = WelcomeError(
                            kind = WelcomeErrorKind.SignInFailed,
                            message = result.message,
                        ),
                    )
                }
            }
        }
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }

    companion object {
        private const val TAG = "WelcomeVM"
    }
}

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val error: WelcomeError? = null,
)

/**
 * Error untuk ditampilkan di snackbar. `message` sudah human-readable
 * (Bahasa Indonesia) dari layer di bawah.
 */
data class WelcomeError(
    val kind: WelcomeErrorKind,
    val message: String,
)

enum class WelcomeErrorKind {
    Canceled,
    SignInFailed,
    AuthFailed,
}
