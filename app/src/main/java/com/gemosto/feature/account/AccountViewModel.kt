package com.gemosto.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.auth.AuthRepository
import com.gemosto.data.prefs.UserPrefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AccountViewModel(
    private val authRepo: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _deleteState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteState: StateFlow<DeleteAccountState> = _deleteState.asStateFlow()

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPrefs.clearOnboardingAndWarnings()
            authRepo.signOut()
            onComplete()
        }
    }

    fun deleteAccount() {
        if (_deleteState.value is DeleteAccountState.Loading) return
        _deleteState.value = DeleteAccountState.Loading

        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")

                // Step 1: Cascade delete subcollections
                val subcollections = listOf("profile", "romResults", "programs", "painLogs")
                for (subcoll in subcollections) {
                    deleteSubcollection(uid, subcoll)
                }

                // Step 2: Delete user root document
                firestore.collection("users").document(uid).delete().await()

                // Step 3: Delete Firebase Auth user
                firebaseAuth.currentUser?.delete()?.await() ?: throw IllegalStateException("No current user")

                // Step 4: Clear local state
                userPrefs.clearAll()
                
                _deleteState.value = DeleteAccountState.Success
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                _deleteState.value = DeleteAccountState.NeedsReAuth
            } catch (e: Exception) {
                _deleteState.value = DeleteAccountState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun deleteSubcollection(uid: String, name: String) {
        val coll = firestore.collection("users").document(uid).collection(name)
        val snapshot = coll.limit(500).get().await()
        if (snapshot.isEmpty) return

        val batch = firestore.batch()
        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()

        // Recursive kalau > 500
        if (snapshot.size() == 500) {
            deleteSubcollection(uid, name)
        }
    }

    fun consumeDeleteState() {
        _deleteState.value = DeleteAccountState.Idle
    }
}

sealed interface DeleteAccountState {
    data object Idle : DeleteAccountState
    data object Loading : DeleteAccountState
    data object Success : DeleteAccountState
    data object NeedsReAuth : DeleteAccountState
    data class Error(val message: String) : DeleteAccountState
}
