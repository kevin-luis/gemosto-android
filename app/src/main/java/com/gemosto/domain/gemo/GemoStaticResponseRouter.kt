package com.gemosto.domain.gemo

/**
 * Router untuk respons lokal yang tidak membutuhkan pemanggilan Gemini.
 */
object GemoStaticResponseRouter {

    fun responseFor(route: GemoStaticResponseRoute): GemoAiResponse {
        return GemoAiResponse(
            scopeStatus = ScopeStatus.OUT_OF_SCOPE,
            riskLevel = RiskLevel.LOW,
            responseType = ResponseType.REFUSAL,
            answer = answerFor(route),
            disclaimer = GemoAiDisclaimers.DEFAULT,
            recommendedAction = RecommendedAction.NONE,
        )
    }

    private fun answerFor(route: GemoStaticResponseRoute): String {
        return when (route) {
            GemoStaticResponseRoute.SPAM_OR_EMPTY ->
                "Silakan tulis pertanyaan singkat tentang osteoartritis lutut, latihan, atau cara mengelola gejala."

            GemoStaticResponseRoute.GENERIC_OUT_OF_SCOPE ->
                "Maaf, Gemo hanya dapat membantu seputar osteoartritis lutut dan kesehatan yang berkaitan langsung dengannya."

            GemoStaticResponseRoute.CODING_REQUEST ->
                "Maaf, Gemo tidak membantu tugas pemrograman. Gemo hanya fokus pada edukasi seputar kesehatan lutut."

            GemoStaticResponseRoute.FINANCIAL_OR_ACTION_REQUEST ->
                "Maaf, Gemo tidak dapat melakukan tindakan seperti transfer uang atau tugas eksternal lain. Gemo hanya membantu edukasi kesehatan lutut."

            GemoStaticResponseRoute.PROMPT_EXTRACTION ->
                "Maaf, saya tidak dapat membagikan konfigurasi internal. Jika Anda ingin, saya bisa membantu pertanyaan seputar osteoartritis lutut."

            GemoStaticResponseRoute.ROLE_OVERRIDE ->
                "Gemo tetap hanya membantu seputar osteoartritis lutut dan kesehatan yang berkaitan langsung dengannya."
        }
    }
}
