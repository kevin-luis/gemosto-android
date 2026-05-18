package com.gemosto.data.firestore

import com.gemosto.domain.gemo.GemoChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreGemoSessionRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : GemoSessionRepository {

    override suspend fun saveMessage(sessionId: String, message: GemoChatMessage) {
        val uid = auth.currentUser?.uid ?: return
        
        val map = hashMapOf<String, Any?>()
        map["id"] = message.id
        map["author"] = message.author.name
        map["text"] = message.text
        map["responseType"] = message.responseType?.name
        map["riskLevel"] = message.riskLevel?.name
        map["disclaimer"] = message.disclaimer
        map["recommendedAction"] = message.recommendedAction?.name
        map["timestamp"] = message.timestamp
        
        try {
            firestore.collection("users").document(uid)
                .collection("gemoSessions").document(sessionId)
                .collection("messages").document(message.id)
                .set(map)
                .await()
        } catch (e: Exception) {
            // Ignore error gracefully, don't crash the app if firestore fails
        }
    }
}
