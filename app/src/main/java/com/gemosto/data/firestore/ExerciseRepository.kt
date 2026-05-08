package com.gemosto.data.firestore

import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseProgram
import com.gemosto.domain.model.ProgramStatus
import com.gemosto.domain.model.RomCategory
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk Exercise Programs. Path: `users/{uid}/programs/{programId}`.
 *
 * Hari 4: hanya `observeActive` yang dipakai di Home Card.
 * Hari 9-11 (spec 003): full CRUD + Gemini narrative integration.
 */
interface ExerciseRepository {

    /**
     * Program aktif terbaru (status = ACTIVE atau BLOCKED_PAIN). Null kalau
     * user belum pernah generate program.
     */
    fun observeActive(uid: String): Flow<ExerciseProgram?>

    suspend fun upsert(program: ExerciseProgram): Result<Unit>

    suspend fun deleteAll(uid: String): Result<Unit>
}

class FirestoreExerciseRepository(
    private val firestore: FirebaseFirestore,
) : ExerciseRepository {

    private fun collection(uid: String) = firestore
        .collection(USERS).document(uid)
        .collection(PROGRAMS)

    override fun observeActive(uid: String): Flow<ExerciseProgram?> = callbackFlow {
        val sub = collection(uid)
            .whereIn("status", listOf(ProgramStatus.ACTIVE.name, ProgramStatus.BLOCKED_PAIN.name))
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.firstOrNull()?.toExerciseProgram())
            }
        awaitClose { sub.remove() }
    }.catch { it.printStackTrace(); emit(null) }

    override suspend fun upsert(program: ExerciseProgram): Result<Unit> = runCatching {
        collection(program.userId).document(program.id)
            .set(program.toFirestoreMap())
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
        private const val PROGRAMS = "programs"
    }
}

private fun ExerciseProgram.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "generatedAt" to generatedAt,
    "basedOnRomId" to basedOnRomId,
    "romCategory" to romCategory.name,
    "level" to level.name,
    "durationWeeks" to durationWeeks,
    "frequencyPerWeek" to frequencyPerWeek,
    "exerciseCount" to exerciseCount,
    "status" to status.name,
    "safetyNote" to safetyNote,
)

private fun DocumentSnapshot.toExerciseProgram(): ExerciseProgram? {
    if (!exists()) return null
    return try {
        ExerciseProgram(
            id = getString("id") ?: id,
            userId = getString("userId").orEmpty(),
            generatedAt = getLong("generatedAt") ?: return null,
            basedOnRomId = getString("basedOnRomId").orEmpty(),
            romCategory = RomCategory.valueOf(getString("romCategory") ?: return null),
            level = ExerciseLevel.valueOf(getString("level") ?: return null),
            durationWeeks = getLong("durationWeeks")?.toInt() ?: 4,
            frequencyPerWeek = getString("frequencyPerWeek").orEmpty(),
            exerciseCount = getLong("exerciseCount")?.toInt() ?: 0,
            status = ProgramStatus.valueOf(getString("status") ?: return null),
            safetyNote = getString("safetyNote"),
        )
    } catch (e: IllegalArgumentException) {
        null
    }
}
