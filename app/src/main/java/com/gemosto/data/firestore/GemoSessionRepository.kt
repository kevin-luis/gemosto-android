package com.gemosto.data.firestore

import com.gemosto.domain.gemo.GemoChatMessage

interface GemoSessionRepository {
    suspend fun saveMessage(sessionId: String, message: GemoChatMessage)
}
