package com.gemosto.feature.gemo

import com.gemosto.domain.gemo.RecommendedAction
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.RiskLevel
import com.gemosto.domain.gemo.SuggestedQuestion

/**
 * Arah pesan pada sesi chat Gemo AI.
 */
enum class GemoChatAuthor {
    USER,
    GEMO,
}

/**
 * Satu pesan dalam sesi chat awal.
 *
 * Metadata respons hanya diisi untuk pesan dari Gemo supaya UI nanti bisa
 * membedakan education/refusal/escalation tanpa mengurai ulang teks jawaban.
 */
data class GemoChatMessage(
    val id: String,
    val author: GemoChatAuthor,
    val text: String,
    val responseType: ResponseType? = null,
    val riskLevel: RiskLevel? = null,
    val disclaimer: String? = null,
    val recommendedAction: RecommendedAction? = null,
)

/**
 * Error yang cukup untuk flow chat awal.
 */
sealed interface GemoChatError {
    data object SendFailed : GemoChatError
}

/**
 * State minimum yang dibutuhkan UI chat awal.
 */
data class GemoChatUiState(
    val isLoading: Boolean = false,
    val messages: List<GemoChatMessage> = emptyList(),
    val error: GemoChatError? = null,
    val suggestedQuestions: List<SuggestedQuestion> = emptyList(),
) {
    val isEmptySession: Boolean
        get() = messages.isEmpty()
}
