package com.gemosto.feature.gemo

import com.gemosto.domain.gemo.GemoChatAuthor
import com.gemosto.domain.gemo.GemoChatMessage
import com.gemosto.domain.gemo.SuggestedQuestion

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
        get() = messages.none { it.author == GemoChatAuthor.USER }
}
