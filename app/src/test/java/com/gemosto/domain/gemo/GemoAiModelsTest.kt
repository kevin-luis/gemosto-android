package com.gemosto.domain.gemo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test ringan untuk menjaga kontrak domain Gemo AI tetap selaras dengan prompt package.
 */
class GemoAiModelsTest {

    @Test
    fun `scope status enum names match prompt package contract`() {
        assertEquals(
            listOf("IN_SCOPE", "RELATED_SCOPE", "OUT_OF_SCOPE"),
            ScopeStatus.entries.map { it.name },
        )
    }

    @Test
    fun `risk level enum names match prompt package contract`() {
        assertEquals(
            listOf("LOW", "CAUTION", "URGENT"),
            RiskLevel.entries.map { it.name },
        )
    }

    @Test
    fun `response type enum names match prompt package contract`() {
        assertEquals(
            listOf("EDUCATION", "REFUSAL", "ESCALATION"),
            ResponseType.entries.map { it.name },
        )
    }

    @Test
    fun `recommended action enum names match prompt package contract`() {
        assertEquals(
            listOf("NONE", "CONSULT_DOCTOR", "SEEK_URGENT_CARE"),
            RecommendedAction.entries.map { it.name },
        )
    }

    @Test
    fun `disclaimer constants match prompt package contract`() {
        assertEquals(
            "Gemo bisa keliru, dan informasi ini bukan pengganti pemeriksaan dokter.",
            GemoAiDisclaimers.DEFAULT,
        )
        assertEquals(
            "Gemo bisa keliru dan tidak dapat menggantikan pemeriksaan atau anjuran dokter.",
            GemoAiDisclaimers.DIAGNOSIS_OR_MEDICATION,
        )
        assertEquals(
            "Gemo bisa keliru, tetapi gejala seperti ini perlu dinilai tenaga medis segera.",
            GemoAiDisclaimers.URGENT,
        )
    }

    @Test
    fun `suggested question keeps static copy text`() {
        assertEquals(
            "Apa itu osteoartritis lutut?",
            SuggestedQuestion(text = "Apa itu osteoartritis lutut?").text,
        )
    }
}
