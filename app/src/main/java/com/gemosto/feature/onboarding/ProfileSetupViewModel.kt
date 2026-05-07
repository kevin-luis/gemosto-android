package com.gemosto.feature.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.firestore.ProfileRepository
import com.gemosto.data.prefs.UserPrefs
import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.AuthState
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel untuk ProfileSetupScreen.
 *
 * Tanggung jawab:
 *  1. Pre-fill nama + email + photoUrl dari AuthState (Google account)
 *  2. Validasi form (umur 18-100, knee + activity terisi, disclaimer ✓)
 *  3. Save profile ke Firestore + set DataStore flag
 *
 * Setelah save sukses → ProfileRepository.observe() di AppViewModel
 * akan emit Loaded → AppRoot routing redirect ke Home secara otomatis.
 */
class ProfileSetupViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository,
    private val userPrefs: UserPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupUiState())
    val state: StateFlow<ProfileSetupUiState> = _state.asStateFlow()

    init {
        prefillFromAuth()
    }

    private fun prefillFromAuth() {
        viewModelScope.launch {
            // Tunggu sampai AuthState SignedIn — biasanya sudah saat screen muncul
            val signedIn = authRepo.authState
                .filterIsInstance<AuthState.SignedIn>()
                .first()
            _state.update { current ->
                current.copy(
                    uid = signedIn.uid,
                    email = signedIn.email,
                    photoUrl = signedIn.photoUrl,
                    // Pre-fill nama hanya kalau user belum ketik apapun
                    nameInput = if (current.nameInput.isBlank()) {
                        signedIn.displayName.orEmpty()
                    } else current.nameInput,
                )
            }
        }
    }

    // ─── Event handlers dari UI ─────────────────────────────────

    fun onNameChange(value: String) {
        _state.update { it.copy(nameInput = value, saveError = null) }
    }

    fun onAgeChange(value: String) {
        // Filter hanya digit, max 3 char
        val digits = value.filter(Char::isDigit).take(3)
        _state.update { it.copy(ageInput = digits, saveError = null) }
    }

    fun onKneeSelected(side: KneeSide) {
        _state.update { it.copy(affectedKnee = side, saveError = null) }
    }

    fun onActivitySelected(level: ActivityLevel) {
        _state.update { it.copy(activityLevel = level, saveError = null) }
    }

    fun onDisclaimerToggle(checked: Boolean) {
        _state.update { it.copy(disclaimerAccepted = checked, saveError = null) }
    }

    fun onSaveClick() {
        val current = _state.value
        if (!current.isFormValid || current.isSaving) return

        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val profile = UserProfile(
                uid = current.uid,
                name = current.nameInput.trim(),
                email = current.email,
                photoUrl = current.photoUrl,
                age = current.ageInt!!,
                affectedKnee = current.affectedKnee!!,
                activityLevel = current.activityLevel!!,
                disclaimerAcceptedAt = now,
                createdAt = 0L,    // di-set di repository kalau masih 0
                updatedAt = now,
            )

            val result = profileRepo.upsert(profile)
            if (result.isSuccess) {
                userPrefs.setOnboardingCompleted(true)
                // Tidak perlu navigasi manual — AppRoot listener akan kick in
                // saat Firestore profile observer emit Loaded.
                _state.update { it.copy(isSaving = false) }
            } else {
                val err = result.exceptionOrNull()
                Log.e(TAG, "Failed to save profile", err)
                _state.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Gagal menyimpan: ${err?.message ?: "tidak diketahui"}",
                    )
                }
            }
        }
    }

    fun consumeError() {
        _state.update { it.copy(saveError = null) }
    }

    companion object {
        private const val TAG = "ProfileSetupVM"
    }
}

/**
 * State form ProfileSetupScreen.
 *
 * `isFormValid` adalah computed: semua field harus terisi valid + disclaimer ✓
 */
data class ProfileSetupUiState(
    // Pre-filled (read-only di UI)
    val uid: String = "",
    val email: String = "",
    val photoUrl: String? = null,

    // Editable
    val nameInput: String = "",
    val ageInput: String = "",
    val affectedKnee: KneeSide? = null,
    val activityLevel: ActivityLevel? = null,
    val disclaimerAccepted: Boolean = false,

    // Status
    val isSaving: Boolean = false,
    val saveError: String? = null,
) {
    val ageInt: Int? get() = ageInput.toIntOrNull()
    val isAgeValid: Boolean get() = ageInt in 18..100
    val isAgeFieldDirty: Boolean get() = ageInput.isNotBlank()
    val isAgeError: Boolean get() = isAgeFieldDirty && !isAgeValid
    val isNameValid: Boolean get() = nameInput.trim().length in 2..50

    val isFormValid: Boolean
        get() = uid.isNotBlank()
            && isNameValid
            && isAgeValid
            && affectedKnee != null
            && activityLevel != null
            && disclaimerAccepted
}
