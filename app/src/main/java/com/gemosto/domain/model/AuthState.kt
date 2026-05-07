package com.gemosto.domain.model

/**
 * Status autentikasi user — dipantau via Flow di AppViewModel
 * untuk menentukan routing splash → welcome / home.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(
        val uid: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
    ) : AuthState
}

/**
 * Status profil user — apakah sudah pernah save ke Firestore.
 * Dipakai bersama AuthState untuk routing.
 */
sealed interface ProfileState {
    data object Loading : ProfileState
    data object Missing : ProfileState
    data class Loaded(val profile: UserProfile) : ProfileState
}
