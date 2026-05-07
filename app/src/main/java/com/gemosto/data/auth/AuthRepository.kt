package com.gemosto.data.auth

import com.gemosto.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Abstraksi authentication. Pure interface — supaya feature layer
 * tidak depend ke Firebase langsung.
 *
 * Implementasi: [FirebaseAuthRepository].
 */
interface AuthRepository {

    /**
     * Real-time Flow dari status autentikasi.
     * Emit [AuthState.SignedIn] kalau user login, [AuthState.SignedOut] kalau tidak.
     */
    val authState: Flow<AuthState>

    /**
     * Sign in dengan Google ID Token (didapat dari Credential Manager).
     * @return success unit kalau OK, error kalau gagal verifikasi token.
     */
    suspend fun signInWithGoogle(googleIdToken: String): Result<Unit>

    /**
     * Sign out — clear Firebase session.
     */
    suspend fun signOut()

    /**
     * Hapus akun user dari Firebase Auth.
     * Caller wajib delete data Firestore SEBELUM panggil ini.
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * UID user saat ini — null kalau tidak login.
     */
    fun currentUid(): String?
}
