package com.gemosto.data.auth

import com.gemosto.domain.model.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementasi [AuthRepository] menggunakan Firebase Auth.
 *
 * `authState` adalah Flow yang mendengarkan [FirebaseAuth.AuthStateListener] —
 * akan emit setiap kali user berubah (login/logout/token refresh).
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null) {
                trySend(AuthState.SignedOut)
            } else {
                trySend(
                    AuthState.SignedIn(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        displayName = user.displayName,
                        photoUrl = user.photoUrl?.toString(),
                    )
                )
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(googleIdToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        Unit
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("No current user")
        user.delete().await()
        Unit
    }

    override fun currentUid(): String? = firebaseAuth.currentUser?.uid
}
