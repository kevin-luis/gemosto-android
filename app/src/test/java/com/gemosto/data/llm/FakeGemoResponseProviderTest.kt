package com.gemosto.data.llm

import com.gemosto.domain.gemo.GemoAiResponseValidator
import com.gemosto.domain.gemo.RecommendedAction
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.RiskLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeGemoResponseProviderTest {

    private val provider = FakeGemoResponseProvider()

    @Test
    fun `education debug response remains valid`() = runTest {
        val response = provider.generate("Apa itu OA lutut?")

        assertEquals(RiskLevel.LOW, response.riskLevel)
        assertEquals(ResponseType.EDUCATION, response.responseType)
        assertTrue(GemoAiResponseValidator.isValid(response))
    }

    @Test
    fun `diagnosis or medication prompt returns caution contract`() = runTest {
        val response = provider.generate("Berapa dosis obat yang cocok untuk saya?")

        assertEquals(RiskLevel.CAUTION, response.riskLevel)
        assertEquals(RecommendedAction.CONSULT_DOCTOR, response.recommendedAction)
        assertTrue(GemoAiResponseValidator.isValid(response))
    }

    @Test
    fun `urgent prompt returns escalation contract`() = runTest {
        val response = provider.generate("Saya tidak bisa menapak dan lutut terasa panas.")

        assertEquals(RiskLevel.URGENT, response.riskLevel)
        assertEquals(ResponseType.ESCALATION, response.responseType)
        assertEquals(RecommendedAction.SEEK_URGENT_CARE, response.recommendedAction)
        assertTrue(GemoAiResponseValidator.isValid(response))
    }
}
