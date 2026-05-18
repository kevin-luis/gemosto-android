package com.gemosto.data.llm

import com.gemosto.domain.gemo.GemoAiResponse
import com.gemosto.domain.gemo.GemoChatMessage

/**
 * Provider jawaban Gemo yang bisa diganti antara model live dan fake provider debug.
 */
interface GemoResponseProvider {
    suspend fun generate(
        userMessage: String,
        history: List<GemoChatMessage> = emptyList(),
    ): GemoAiResponse
}
