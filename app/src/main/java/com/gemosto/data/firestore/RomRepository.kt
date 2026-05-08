package com.gemosto.data.firestore

import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.RomCategory
import com.gemosto.domain.model.RomResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk hasil ROM. Path: `users/{uid}/romResults/{romId}`.
 *
 * MVP: limit hard-cap 50 entry untuk hemat read quota Spark plan.
 * Hari 13 (spec 007): pagination kalau perlu.
 */
interface RomRepository {

    /** Real-time observe semua hasil ROM, sorted DESC by timestamp. Limit 50. */
    fun observeAll(uid: String): Flow<List<RomResult>>

    /** Real-time observe hasil ROM terbaru (1 entry) — dipakai di Home. */
    fun observeLatest(uid: String): Flow<RomResult?>

    suspend fun get(uid: String, romId: String): RomResult?

    suspend fun upsert(result: RomResult): Result<Unit>

    suspend fun deleteAll(uid: String): Result<Unit>
}

class FirestoreRomRepository(
    private val firestore: FirebaseFirestore,
) : RomRepository {

    private fun collection(uid: String) = firestore
        .collection(USERS).document(uid)
        .collection(ROM_RESULTS)

    override fun observeAll(uid: String): Flow<List<RomResult>> = callbackFlow {
        val sub = collection(uid)
            .orderBy("timestampMs", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val items = snap?.documents?.mapNotNull { it.toRomResult() }.orEmpty()
                trySend(items)
            }
        awaitClose { sub.remove() }
    }.catchToEmpty()

    override fun observeLatest(uid: String): Flow<RomResult?> = callbackFlow {
        val sub = collection(uid)
            .orderBy("timestampMs", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val first = snap?.documents?.firstOrNull()?.toRomResult()
                trySend(first)
            }
        awaitClose { sub.remove() }
    }.catchToNull()

    override suspend fun get(uid: String, romId: String): RomResult? {
        return collection(uid).document(romId).get().await().toRomResult()
    }

    override suspend fun upsert(result: RomResult): Result<Unit> = runCatching {
        collection(result.userId).document(result.id)
            .set(result.toFirestoreMap())
            .await()
        Unit
    }

    override suspend fun deleteAll(uid: String): Result<Unit> = runCatching {
        val snapshot = collection(uid).get().await()
        if (snapshot.isEmpty) return@runCatching Unit
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Unit
    }

    companion object {
        private const val USERS = "users"
        private const val ROM_RESULTS = "romResults"
    }
}

// ─── Mapping ─────────────────────────────────────────────────────

private fun RomResult.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "timestampMs" to timestampMs,
    "kneeSide" to kneeSide.name,
    "maxFlexionDeg" to maxFlexionDeg,
    "maxExtensionLagDeg" to maxExtensionLagDeg,
    "category" to category.name,
    "sessionDurationMs" to sessionDurationMs,
    "deviceModel" to deviceModel,
    "mediaPipeModel" to mediaPipeModel,
)

private fun DocumentSnapshot.toRomResult(): RomResult? {
    if (!exists()) return null
    return try {
        RomResult(
            id = getString("id") ?: id,
            userId = getString("userId").orEmpty(),
            timestampMs = getLong("timestampMs") ?: return null,
            kneeSide = KneeSide.valueOf(getString("kneeSide") ?: return null),
            maxFlexionDeg = getDouble("maxFlexionDeg")?.toFloat() ?: return null,
            maxExtensionLagDeg = getDouble("maxExtensionLagDeg")?.toFloat() ?: return null,
            category = RomCategory.valueOf(getString("category") ?: return null),
            sessionDurationMs = getLong("sessionDurationMs") ?: 0L,
            deviceModel = getString("deviceModel").orEmpty(),
            mediaPipeModel = getString("mediaPipeModel").orEmpty(),
        )
    } catch (e: IllegalArgumentException) {
        null
    }
}

// ─── Defensive Flow extensions ────────────────────────────────────
// Kalau Firestore Security Rules belum di-deploy → PERMISSION_DENIED.
// Daripada crash, emit empty/null supaya UI tetap render dengan empty state.

private fun Flow<List<RomResult>>.catchToEmpty(): Flow<List<RomResult>> =
    this.catch { it.printStackTrace(); emit(emptyList()) }

private fun Flow<RomResult?>.catchToNull(): Flow<RomResult?> =
    this.catch { it.printStackTrace(); emit(null) }
