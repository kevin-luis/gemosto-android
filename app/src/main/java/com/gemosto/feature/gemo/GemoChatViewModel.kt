package com.gemosto.feature.gemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemosto.data.firestore.GemoSessionRepository
import com.gemosto.data.llm.GemoAiRepository
import com.gemosto.domain.gemo.GemoAiResponse
import com.gemosto.domain.gemo.GemoChatAuthor
import com.gemosto.domain.gemo.GemoChatMessage
import com.gemosto.domain.gemo.RecommendedAction
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.RiskLevel
import com.gemosto.domain.gemo.SuggestedQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * State holder untuk sesi chat Gemo AI tahap awal.
 *
 * ViewModel hanya mengorkestrasi event UI dan repository. Prompt panjang,
 * prefilter, dan integrasi Gemini tetap berada di layer lain.
 */
class GemoChatViewModel(
    private val gemoAiRepository: GemoAiRepository,
    private val gemoSessionRepository: GemoSessionRepository,
    private val initialSuggestedQuestions: List<SuggestedQuestion>,
) : ViewModel() {

    private var activeRequestJob: Job? = null
    private var lastFailedPrompt: String? = null
    private var nextMessageSequence: Long = 0L
    private var sessionId: String = UUID.randomUUID().toString()

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<GemoChatUiState> = _state.asStateFlow()

    private fun createInitialState(): GemoChatUiState {
        val greetingMessage = GemoChatMessage(
            id = "gemo_greeting",
            author = GemoChatAuthor.GEMO,
            text = "Halo, saya Gemo, Asisten Kesehatan Lutut Anda.",
            responseType = ResponseType.EDUCATION,
            riskLevel = RiskLevel.LOW,
            recommendedAction = RecommendedAction.NONE,
        )
        return GemoChatUiState(
            messages = listOf(greetingMessage),
            suggestedQuestions = initialSuggestedQuestions,
        )
    }

    fun onSuggestedQuestionClick(question: SuggestedQuestion) {
        onSend(question.text)
    }

    fun onSend(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank() || _state.value.isLoading) return

        val userMsg = text.toUserMessage()
        _state.update { current ->
            current.copy(
                isLoading = true,
                error = null,
                messages = current.messages + userMsg,
                suggestedQuestions = emptyList(),
            )
        }
        
        viewModelScope.launch {
            gemoSessionRepository.saveMessage(sessionId, userMsg)
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
        sessionId = UUID.randomUUID().toString()
        _state.value = createInitialState()
    }

    private fun requestAssistantResponse(prompt: String) {
        // Drop the last message (which is the current user prompt we just added)
        // and filter out the static greeting.
        val historyToPass = _state.value.messages
            .dropLast(1)
            .filter { it.id != "gemo_greeting" }

        activeRequestJob = viewModelScope.launch {
            try {
                val response = gemoAiRepository.generateResponse(prompt, historyToPass)
                val gemoMsg = response.toGemoMessage()
                
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        error = null,
                        messages = current.messages + gemoMsg,
                    )
                }
                
                gemoSessionRepository.saveMessage(sessionId, gemoMsg)
                
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
