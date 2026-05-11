package com.gemosto.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.firestore.ProfileRepository
import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileEditViewModel(
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    fun initProfile(profile: UserProfile) {
        if (_state.value.isInitialized) return
        _state.value = _state.value.copy(
            isInitialized = true,
            name = profile.name,
            age = profile.age?.toString() ?: "",
            kneeSide = profile.affectedKnee,
            activityLevel = profile.activityLevel
        )
    }

    fun onNameChange(name: String) {
        _state.value = _state.value.copy(name = name)
        validate()
    }

    fun onAgeChange(age: String) {
        if (age.isEmpty() || age.all { it.isDigit() }) {
            _state.value = _state.value.copy(age = age)
            validate()
        }
    }

    fun onKneeSideSelect(side: KneeSide) {
        _state.value = _state.value.copy(kneeSide = side)
        validate()
    }

    fun onActivityLevelSelect(level: ActivityLevel) {
        _state.value = _state.value.copy(activityLevel = level)
        validate()
    }

    private fun validate() {
        val s = _state.value
        val isNameValid = s.name.length in 2..50
        val ageInt = s.age.toIntOrNull()
        val isAgeValid = ageInt != null && ageInt in 18..100
        val isValid = isNameValid && isAgeValid && s.kneeSide != null && s.activityLevel != null

        _state.value = s.copy(isValid = isValid)
    }

    fun saveProfile(profile: UserProfile, onComplete: () -> Unit) {
        if (!_state.value.isValid) return
        _state.value = _state.value.copy(isSaving = true)

        viewModelScope.launch {
            val s = _state.value
            val updatedProfile = profile.copy(
                name = s.name,
                age = s.age.toIntOrNull() ?: profile.age,
                affectedKnee = s.kneeSide ?: profile.affectedKnee,
                activityLevel = s.activityLevel ?: profile.activityLevel,
                updatedAt = System.currentTimeMillis()
            )
            val result = profileRepo.upsert(updatedProfile)
            _state.value = _state.value.copy(isSaving = false, saveSuccess = result.isSuccess)
            if (result.isSuccess) {
                onComplete()
            }
        }
    }
}

data class ProfileEditState(
    val isInitialized: Boolean = false,
    val name: String = "",
    val age: String = "",
    val kneeSide: KneeSide? = null,
    val activityLevel: ActivityLevel? = null,
    val isValid: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)
