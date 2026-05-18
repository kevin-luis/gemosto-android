package com.gemosto.feature.gemo

import com.gemosto.data.llm.GemoAiRepository
import com.gemosto.data.firestore.GemoSessionRepository
import com.gemosto.domain.gemo.GemoAiDisclaimers
import com.gemosto.domain.gemo.GemoAiResponse
import com.gemosto.domain.gemo.GemoChatAuthor
import com.gemosto.domain.gemo.RecommendedAction
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.RiskLevel
import com.gemosto.domain.gemo.ScopeStatus
import com.gemosto.domain.gemo.SuggestedQuestion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class GemoChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<GemoAiRepository>()
    private val sessionRepository = mockk<GemoSessionRepository>(relaxed = true)
    private val suggestedQuestions = listOf(
        SuggestedQuestion("Apa itu osteoartritis lutut?"),
        SuggestedQuestion("Latihan ringan apa yang umumnya baik untuk OA lutut?"),
    )

    @Test
    fun `initial state is empty and shows suggested questions`() {
        val viewModel = GemoChatViewModel(repository, sessionRepository, suggestedQuestions)

        assertTrue(viewModel.state.value.isEmptySession)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(suggestedQuestions, viewModel.state.value.suggestedQuestions)
        assertNull(viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.messages.size)
    }

    @Test
    fun `suggested question click starts chat and appends gemo response`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val expectedResponse = validEducationResponse()
            coEvery {
                repository.generateResponse("Apa itu osteoartritis lutut?", emptyList())
            } returns expectedResponse
            val viewModel = GemoChatViewModel(repository, sessionRepository, suggestedQuestions)

            viewModel.onSuggestedQuestionClick(suggestedQuestions.first())
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isEmptySession)
            assertFalse(state.isLoading)
            assertTrue(state.suggestedQuestions.isEmpty())
            assertEquals(3, state.messages.size)
            assertEquals(GemoChatAuthor.USER, state.messages[1].author)
            assertEquals(GemoChatAuthor.GEMO, state.messages[2].author)
            assertEquals(ResponseType.EDUCATION, state.messages[2].responseType)
            coVerify(exactly = 1) {
                repository.generateResponse("Apa itu osteoartritis lutut?", emptyList())
            }
        }

    @Test
    fun `retry resubmits last failed prompt without duplicating user message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prompt = "Apa itu OA lutut?"
            coEvery { repository.generateResponse(prompt, emptyList()) } throws IllegalStateException("network")
            val viewModel = GemoChatViewModel(repository, sessionRepository, suggestedQuestions)

            viewModel.onSend(prompt)
            advanceUntilIdle()

            assertEquals(GemoChatError.SendFailed, viewModel.state.value.error)
            assertEquals(2, viewModel.state.value.messages.size)

            coEvery { repository.generateResponse(prompt, emptyList()) } returns validEducationResponse()
            viewModel.onRetry()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNull(state.error)
            assertEquals(3, state.messages.size)
            assertEquals(GemoChatAuthor.USER, state.messages[1].author)
            assertEquals(GemoChatAuthor.GEMO, state.messages[2].author)
            coVerify(exactly = 2) { repository.generateResponse(prompt, emptyList()) }
        }

    @Test
    fun `new session resets messages error and suggested questions`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { repository.generateResponse(any(), any()) } returns validEducationResponse()
            val viewModel = GemoChatViewModel(repository, sessionRepository, suggestedQuestions)

            viewModel.onSend("Apa itu OA lutut?")
            advanceUntilIdle()
            viewModel.onNewSession()

            val state = viewModel.state.value
            assertTrue(state.isEmptySession)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(suggestedQuestions, state.suggestedQuestions)
            assertEquals(1, state.messages.size)
        }

    private fun validEducationResponse(): GemoAiResponse {
        return GemoAiResponse(
            scopeStatus = ScopeStatus.IN_SCOPE,
            riskLevel = RiskLevel.LOW,
            responseType = ResponseType.EDUCATION,
            answer = "Osteoartritis lutut adalah perubahan bertahap pada sendi lutut.",
            disclaimer = GemoAiDisclaimers.DEFAULT,
            recommendedAction = RecommendedAction.NONE,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
