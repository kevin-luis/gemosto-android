package com.gemosto.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.firestore.ExerciseRepository
import com.gemosto.data.firestore.PainLogRepository
import com.gemosto.data.firestore.RomRepository
import com.gemosto.data.prefs.UserPrefs
import com.gemosto.domain.model.AuthState
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.RomResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder Home Dashboard.
 *
 * Combines: latest ROM result + active program — keduanya optional.
 * Profile diterima dari caller (sudah loaded di AppRoot routing) supaya
 * tidak perlu re-observe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    authRepo: AuthRepository,
    romRepo: RomRepository,
    exerciseRepo: ExerciseRepository,
    private val painLogRepo: PainLogRepository,
    private val userPrefs: UserPrefs,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = authRepo.authState
        .flatMapLatest { auth ->
            when (auth) {
                is AuthState.SignedIn -> combine(
                    romRepo.observeLatest(auth.uid),
                    exerciseRepo.observeActive(auth.uid),
                    painLogRepo.recentEntries(1),
                    userPrefs.painWarningDismissedAt
                ) { latestRom, activeProgram, recentPainLogs, dismissedAt ->
                    val latestPain = recentPainLogs.firstOrNull()
                    val showPainWarning = latestPain != null && 
                                          latestPain.score >= 7 && 
                                          latestPain.timestampMs > dismissedAt
                    
                    HomeUiState(
                        isLoading = false,
                        latestRom = latestRom,
                        activeProgram = activeProgram,
                        showPainWarning = showPainWarning
                    )
                }
                else -> flowOf(HomeUiState(isLoading = true))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true),
        )

    fun dismissPainWarning() {
        viewModelScope.launch {
            userPrefs.setPainWarningDismissedAt(System.currentTimeMillis())
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val latestRom: RomResult? = null,
    val activeProgram: ExerciseProgram? = null,
    val showPainWarning: Boolean = false,
)
