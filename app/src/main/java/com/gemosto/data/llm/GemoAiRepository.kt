package com.gemosto.data.llm

import com.gemosto.domain.gemo.GemoAiResponse
import com.gemosto.domain.gemo.GemoChatMessage

/**
 * Abstraksi jalur chat Gemo AI untuk feature layer.
 *
 * UI cukup mengirim teks dan menerima respons aman, tanpa tahu apakah hasilnya
 * berasal dari prefilter lokal atau panggilan Gemini.
 */
interface GemoAiRepository {

    suspend fun generateResponse(
        userMessage: String,
        history: List<GemoChatMessage> = emptyList()
    ): GemoAiResponse
}
