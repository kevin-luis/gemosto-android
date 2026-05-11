package com.gemosto.data.firestore

import com.gemosto.domain.model.PainEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface PainLogRepository {
    suspend fun save(entry: PainEntry): Result<Unit>
    fun recentEntries(limit: Int): Flow<List<PainEntry>>
}

class FirestorePainLogRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PainLogRepository {

    override suspend fun save(entry: PainEntry): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        firestore.collection("users").document(uid)
            .collection("painLogs").document(entry.id)
            .set(entry.toFirestoreMap())
            .await()
    }

    override fun recentEntries(limit: Int): Flow<List<PainEntry>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { 
            close()
            return@callbackFlow 
        }
        val sub = firestore.collection("users").document(uid)
            .collection("painLogs")
            .orderBy("timestampMs", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snap, err ->
                if (err != null) { 
                    close(err) 
                    return@addSnapshotListener 
                }
                val entries = snap?.documents?.mapNotNull { doc -> 
                    try {
                        PainEntry(
                            id = doc.getString("id") ?: return@mapNotNull null,
                            userId = doc.getString("userId") ?: return@mapNotNull null,
                            timestampMs = doc.getLong("timestampMs") ?: return@mapNotNull null,
                            score = doc.getLong("score")?.toInt() ?: return@mapNotNull null,
                            stoppedDueToPain = doc.getBoolean("stoppedDueToPain") ?: false,
                            programId = doc.getString("programId"),
                            sessionStartedAtMs = doc.getLong("sessionStartedAtMs"),
                            sessionDurationMs = doc.getLong("sessionDurationMs")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(entries)
            }
        awaitClose { sub.remove() }
    }
}

fun PainEntry.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "timestampMs" to timestampMs,
        "score" to score,
        "stoppedDueToPain" to stoppedDueToPain,
        "programId" to programId,
        "sessionStartedAtMs" to sessionStartedAtMs,
        "sessionDurationMs" to sessionDurationMs
    )
}
