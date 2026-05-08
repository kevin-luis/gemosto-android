package com.gemosto.feature.scan

import androidx.lifecycle.ViewModel
import com.gemosto.domain.model.KneeSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State holder untuk flow Scan ROM.
 *
 * Hari 5: hanya `selectedKneeSide` (yang akan diukur di camera).
 * Hari 6-8: tambah scan session state, samples, hasil pengukuran.
 */
class ScanViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    /**
     * Set default knee side berdasar profile.affectedKnee.
     * Kalau BOTH → default RIGHT (sesuai spec 002).
     */
    fun initFromProfile(affected: KneeSide) {
        if (_state.value.selectedKneeSide != null) return  // already initialized
        _state.value = _state.value.copy(
            selectedKneeSide = when (affected) {
                KneeSide.LEFT -> KneeSide.LEFT
                KneeSide.RIGHT -> KneeSide.RIGHT
                KneeSide.BOTH -> KneeSide.RIGHT
            },
        )
    }

    fun onKneeSelected(side: KneeSide) {
        if (side == KneeSide.BOTH) return  // tidak valid untuk pengukuran
        _state.value = _state.value.copy(selectedKneeSide = side)
    }
}

data class ScanUiState(
    val selectedKneeSide: KneeSide? = null,
)
