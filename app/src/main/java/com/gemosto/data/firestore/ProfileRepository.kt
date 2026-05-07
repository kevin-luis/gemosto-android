package com.gemosto.data.firestore

import com.gemosto.domain.model.ActivityLevel
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Abstraksi profile data di Firestore. Path: `users/{uid}/profile/data`.
 *
 * Profile tersimpan sebagai single document di sub-collection `profile`
 * dengan ID `data` — alasan: konvensi yang membatasi 1 profile per user
 * dan compatible dengan Firestore Security Rules per-subcollection.
 */
interface ProfileRepository {
    /**
     * Real-time observer untuk profil current user.
     * Emit null kalau belum ada profile.
     * Caller wajib lewatkan UID — untuk menghindari ambiguity saat re-login.
     */
    fun observe(uid: String): Flow<UserProfile?>

    /**
     * Snapshot read sekali (bukan listener).
     */
    suspend fun get(uid: String): UserProfile?

    /**
     * Upsert profil. ID dokumen selalu "data" (singleton subcoll).
     */
    suspend fun upsert(profile: UserProfile): Result<Unit>

    /**
     * Hapus dokumen profile (dipakai di flow Hapus Akun, sebelum delete user).
     */
    suspend fun delete(uid: String): Result<Unit>
}

class FirestoreProfileRepository(
    private val firestore: FirebaseFirestore,
) : ProfileRepository {

    private fun docRef(uid: String) = firestore
        .collection(USERS).document(uid)
        .collection(PROFILE).document(DOC_ID)

    override fun observe(uid: String): Flow<UserProfile?> = callbackFlow {
        val sub = docRef(uid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toUserProfile())
        }
        awaitClose { sub.remove() }
    }

    override suspend fun get(uid: String): UserProfile? {
        val snap = docRef(uid).get().await()
        return snap.toUserProfile()
    }

    override suspend fun upsert(profile: UserProfile): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val data = profile.toFirestoreMap().toMutableMap().apply {
            put("updatedAt", now)
            // createdAt hanya di-set kalau belum ada (preserve original)
            if (profile.createdAt == 0L) put("createdAt", now)
        }
        docRef(profile.uid).set(data).await()
        Unit
    }

    override suspend fun delete(uid: String): Result<Unit> = runCatching {
        docRef(uid).delete().await()
        Unit
    }

    companion object {
        private const val USERS = "users"
        private const val PROFILE = "profile"
        private const val DOC_ID = "data"
    }
}

// ─── Mapping helpers (kept private to data layer) ──────────────────

private fun UserProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "name" to name,
    "email" to email,
    "photoUrl" to photoUrl,
    "age" to age,
    "affectedKnee" to affectedKnee.name,
    "activityLevel" to activityLevel.name,
    "disclaimerAcceptedAt" to disclaimerAcceptedAt,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

private fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    return try {
        UserProfile(
            uid = getString("uid").orEmpty(),
            name = getString("name").orEmpty(),
            email = getString("email").orEmpty(),
            photoUrl = getString("photoUrl"),
            age = getLong("age")?.toInt() ?: return null,
            affectedKnee = KneeSide.valueOf(getString("affectedKnee") ?: return null),
            activityLevel = ActivityLevel.valueOf(getString("activityLevel") ?: return null),
            disclaimerAcceptedAt = getLong("disclaimerAcceptedAt") ?: 0L,
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L,
        )
    } catch (e: IllegalArgumentException) {
        // enum valueOf gagal — schema corrupt
        null
    }
}
