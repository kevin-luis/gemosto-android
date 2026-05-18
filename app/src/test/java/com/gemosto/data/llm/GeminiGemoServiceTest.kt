package com.gemosto.data.llm

import com.gemosto.domain.gemo.GemoAiDisclaimers
import com.gemosto.domain.gemo.GemoAiResponseValidator
import com.gemosto.domain.gemo.RecommendedAction
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.RiskLevel
import com.gemosto.domain.gemo.ScopeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiGemoServiceTest {

    @Test
    fun `parse response json maps valid response`() {
        val response = GeminiGemoService.parseResponseJson(
            """
            {
              "scope_status": "related_scope",
              "risk_level": "low",
              "response_type": "education",
              "answer": "Berat badan dapat memengaruhi OA lutut karena lutut menahan beban tubuh.",
              "disclaimer": "Gemo bisa keliru, dan informasi ini bukan pengganti pemeriksaan dokter.",
              "recommended_action": "none"
            }
            """.trimIndent(),
        )

        assertEquals(ScopeStatus.RELATED_SCOPE, response.scopeStatus)
        assertEquals(RiskLevel.LOW, response.riskLevel)
        assertEquals(ResponseType.EDUCATION, response.responseType)
        assertEquals(RecommendedAction.NONE, response.recommendedAction)
    }

    @Test(expected = IllegalStateException::class)
    fun `parse response json rejects unknown enum`() {
        GeminiGemoService.parseResponseJson(
            """
            {
              "scope_status": "in_scope",
              "risk_level": "medium",
              "response_type": "education",
              "answer": "Jawaban aman.",
              "disclaimer": "Gemo bisa keliru, dan informasi ini bukan pengganti pemeriksaan dokter.",
              "recommended_action": "none"
            }
            """.trimIndent(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse response json rejects urgent response without urgent action`() {
        GeminiGemoService.parseResponseJson(
            """
            {
              "scope_status": "in_scope",
              "risk_level": "urgent",
              "response_type": "escalation",
              "answer": "Silakan cari pertolongan medis segera.",
              "disclaimer": "Gemo bisa keliru, tetapi gejala seperti ini perlu dinilai tenaga medis segera.",
              "recommended_action": "none"
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `fallback response is safe refusal`() {
        val response = GeminiGemoService.fallbackResponse()

        assertEquals(ScopeStatus.OUT_OF_SCOPE, response.scopeStatus)
        assertEquals(RiskLevel.LOW, response.riskLevel)
        assertEquals(ResponseType.REFUSAL, response.responseType)
        assertEquals(RecommendedAction.NONE, response.recommendedAction)
        assertEquals(GemoAiDisclaimers.DEFAULT, response.disclaimer)
        assertTrue(response.answer.isNotBlank())
        assertTrue(GemoAiResponseValidator.isValid(response))
    }
}
