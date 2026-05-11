package com.gemosto.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.firestore.RomRepository
import com.gemosto.domain.model.AuthState
import com.gemosto.domain.model.RomResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    authRepo: AuthRepository,
    romRepo: RomRepository
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = authRepo.authState
        .flatMapLatest { auth ->
            when (auth) {
                is AuthState.SignedIn -> romRepo.observeAll(auth.uid).map { items ->
                    HistoryUiState(isLoading = false, items = items)
                }
                else -> flowOf(HistoryUiState(isLoading = true, items = emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(isLoading = true, items = emptyList())
        )
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<RomResult> = emptyList(),
    val error: String? = null
)
