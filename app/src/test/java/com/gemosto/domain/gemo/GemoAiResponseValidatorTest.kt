package com.gemosto.domain.gemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GemoAiResponseValidatorTest {

    @Test
    fun `valid education response is accepted`() {
        assertTrue(GemoAiResponseValidator.isValid(validEducationResponse()))
    }

    @Test
    fun `blank disclaimer is rejected`() {
        val issues = GemoAiResponseValidator.issuesFor(
            validEducationResponse().copy(disclaimer = ""),
        )

        assertFalse(GemoAiResponseValidator.isValid(validEducationResponse().copy(disclaimer = "")))
        assertEquals(
            setOf(GemoAiResponseValidationIssue.BLANK_DISCLAIMER),
            issues,
        )
    }

    @Test
    fun `out of scope education response is rejected`() {
        val issues = GemoAiResponseValidator.issuesFor(
            validEducationResponse().copy(scopeStatus = ScopeStatus.OUT_OF_SCOPE),
        )

        assertEquals(
            setOf(GemoAiResponseValidationIssue.OUT_OF_SCOPE_REQUIRES_REFUSAL),
            issues,
        )
    }

    @Test
    fun `urgent response without escalation is rejected`() {
        val issues = GemoAiResponseValidator.issuesFor(
            validEducationResponse().copy(
                riskLevel = RiskLevel.URGENT,
                recommendedAction = RecommendedAction.SEEK_URGENT_CARE,
            ),
        )

        assertEquals(
            setOf(GemoAiResponseValidationIssue.URGENT_REQUIRES_ESCALATION),
            issues,
        )
    }

    @Test
    fun `urgent response without seek urgent care action is rejected`() {
        val issues = GemoAiResponseValidator.issuesFor(
            validEducationResponse().copy(
                riskLevel = RiskLevel.URGENT,
                responseType = ResponseType.ESCALATION,
            ),
        )

        assertEquals(
            setOf(GemoAiResponseValidationIssue.URGENT_REQUIRES_SEEK_URGENT_CARE),
            issues,
        )
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
