package com.gemosto.domain.gemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemoStaticResponseRouterTest {

    @Test
    fun `all static routes return valid refusal envelopes`() {
        GemoStaticResponseRoute.entries.forEach { route ->
            val response = GemoStaticResponseRouter.responseFor(route)

            assertEquals(ScopeStatus.OUT_OF_SCOPE, response.scopeStatus)
            assertEquals(RiskLevel.LOW, response.riskLevel)
            assertEquals(ResponseType.REFUSAL, response.responseType)
            assertEquals(RecommendedAction.NONE, response.recommendedAction)
            assertEquals(GemoAiDisclaimers.DEFAULT, response.disclaimer)
            assertTrue(response.answer.isNotBlank())
            assertTrue(GemoAiResponseValidator.isValid(response))
        }
    }

    @Test
    fun `coding route uses coding refusal copy`() {
        assertEquals(
            "Maaf, Gemo tidak membantu tugas pemrograman. Gemo hanya fokus pada edukasi seputar kesehatan lutut.",
            GemoStaticResponseRouter.responseFor(GemoStaticResponseRoute.CODING_REQUEST).answer,
        )
    }
}
