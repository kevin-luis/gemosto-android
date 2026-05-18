package com.gemosto.feature.gemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.llm.GemoAiRepository
import com.gemosto.domain.gemo.GemoAiResponse
import com.gemosto.domain.gemo.SuggestedQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State holder untuk sesi chat Gemo AI tahap awal.
 *
 * ViewModel hanya mengorkestrasi event UI dan repository. Prompt panjang,
 * prefilter, dan integrasi Gemini tetap berada di layer lain.
 */
class GemoChatViewModel(
    private val gemoAiRepository: GemoAiRepository,
    private val initialSuggestedQuestions: List<SuggestedQuestion>,
) : ViewModel() {

    private val _state = MutableStateFlow(
        GemoChatUiState(
            suggestedQuestions = initialSuggestedQuestions,
        ),
    )
    val state: StateFlow<GemoChatUiState> = _state.asStateFlow()

    private var activeRequestJob: Job? = null
    private var lastFailedPrompt: String? = null
    private var nextMessageSequence: Long = 0L

    fun onSuggestedQuestionClick(question: SuggestedQuestion) {
        onSend(question.text)
    }

    fun onSend(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank() || _state.value.isLoading) return

        _state.update { current ->
            current.copy(
                isLoading = true,
                error = null,
                messages = current.messages + text.toUserMessage(),
                suggestedQuestions = emptyList(),
            )
        }
        lastFailedPrompt = text
        requestAssistantResponse(prompt = text)
    }

    fun onRetry() {
        val prompt = lastFailedPrompt ?: return
        if (_state.value.isLoading) return

        _state.update { current ->
            current.copy(
                isLoading = true,
                error = null,
            )
        }
        requestAssistantResponse(prompt = prompt)
    }

    fun onNewSession() {
        activeRequestJob?.cancel()
        activeRequestJob = null
        lastFailedPrompt = null
        nextMessageSequence = 0L
        _state.value = GemoChatUiState(
            suggestedQuestions = initialSuggestedQuestions,
        )
    }

    private fun requestAssistantResponse(prompt: String) {
        activeRequestJob = viewModelScope.launch {
            try {
                val response = gemoAiRepository.generateResponse(prompt)
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        error = null,
                        messages = current.messages + response.toGemoMessage(),
                    )
                }
                lastFailedPrompt = null
                activeRequestJob = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        error = GemoChatError.SendFailed,
                    )
                }
                activeRequestJob = null
            }
        }
    }

    private fun String.toUserMessage(): GemoChatMessage {
        return GemoChatMessage(
            id = nextMessageId(prefix = "user"),
            author = GemoChatAuthor.USER,
            text = this,
        )
    }

    private fun GemoAiResponse.toGemoMessage(): GemoChatMessage {
        return GemoChatMessage(
            id = nextMessageId(prefix = "gemo"),
            author = GemoChatAuthor.GEMO,
            text = answer,
            responseType = responseType,
            riskLevel = riskLevel,
            disclaimer = disclaimer,
            recommendedAction = recommendedAction,
        )
    }

    private fun nextMessageId(prefix: String): String {
        nextMessageSequence += 1
        return "${prefix}_$nextMessageSequence"
    }
}
