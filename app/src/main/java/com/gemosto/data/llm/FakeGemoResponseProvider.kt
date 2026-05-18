package com.gemosto.data.llm

import com.gemosto.domain.gemo.GemoAiDisclaimers
import com.gemosto.domain.gemo.GemoAiResponse
import com.gemosto.domain.gemo.GemoChatMessage
import com.gemosto.domain.gemo.RecommendedAction
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.RiskLevel
import com.gemosto.domain.gemo.ScopeStatus

/**
 * Provider lokal untuk debug UI / persistence tanpa menghabiskan quota Gemini.
 *
 * Provider ini tidak dipakai untuk evaluasi kualitas model. Ia hanya menjaga agar
 * flow aplikasi bisa diuji dengan kontrak [GemoAiResponse] yang sama.
 */
class FakeGemoResponseProvider : GemoResponseProvider {

    override suspend fun generate(
        userMessage: String,
        history: List<GemoChatMessage>,
    ): GemoAiResponse {
        val normalized = userMessage.lowercase()

        return when {
            normalized.containsAny(URGENT_HINTS) -> urgentResponse()
            normalized.containsAny(CAUTION_HINTS) -> cautionResponse()
            else -> educationResponse()
        }
    }

    private fun educationResponse(): GemoAiResponse {
        return GemoAiResponse(
            scopeStatus = ScopeStatus.IN_SCOPE,
            riskLevel = RiskLevel.LOW,
            responseType = ResponseType.EDUCATION,
            answer = "Mode debug aktif. Ini contoh jawaban edukasi umum tentang OA lutut untuk menguji alur aplikasi tanpa memanggil model live.",
            disclaimer = GemoAiDisclaimers.DEFAULT,
            recommendedAction = RecommendedAction.NONE,
        )
    }

    private fun cautionResponse(): GemoAiResponse {
        return GemoAiResponse(
            scopeStatus = ScopeStatus.IN_SCOPE,
            riskLevel = RiskLevel.CAUTION,
            responseType = ResponseType.EDUCATION,
            answer = "Mode debug aktif. Gemo tidak dapat menentukan diagnosis, grade, obat, atau dosis personal dari percakapan saja.",
            disclaimer = GemoAiDisclaimers.DIAGNOSIS_OR_MEDICATION,
            recommendedAction = RecommendedAction.CONSULT_DOCTOR,
        )
    }

    private fun urgentResponse(): GemoAiResponse {
        return GemoAiResponse(
            scopeStatus = ScopeStatus.IN_SCOPE,
            riskLevel = RiskLevel.URGENT,
            responseType = ResponseType.ESCALATION,
            answer = "Mode debug aktif. Gejala seperti ini perlu dinilai tenaga medis segera.",
            disclaimer = GemoAiDisclaimers.URGENT,
            recommendedAction = RecommendedAction.SEEK_URGENT_CARE,
        )
    }

    private fun String.containsAny(phrases: List<String>): Boolean = phrases.any(::contains)

    private companion object {
        val URGENT_HINTS = listOf(
            "tidak bisa menapak",
            "tidak bisa digerakkan",
            "panas",
            "demam",
            "bentuknya berubah",
            "jatuh keras",
        )

        val CAUTION_HINTS = listOf(
            "pasti oa",
            "grade berapa",
            "obat",
            "dosis",
        )
    }
}
