package com.gemosto.data.llm

import android.util.Log
import com.gemosto.domain.gemo.GemoAiResponse
import com.gemosto.domain.gemo.GemoAiResponseValidator
import com.gemosto.domain.gemo.GemoScopePrefilter
import com.gemosto.domain.gemo.GemoStaticResponseRoute
import com.gemosto.domain.gemo.GemoStaticResponseRouter
import com.gemosto.domain.gemo.PromptInjectionPrefilter

/**
 * Orkestrator jalur Gemo AI:
 *
 * input user
 * → prefilter lokal
 * → static response atau Gemini
 * → validasi akhir
 * → hasil aman
 */
class GemoAiRepositoryImpl(
    private val geminiGemoService: GeminiGemoService,
) : GemoAiRepository {

    override suspend fun generateResponse(userMessage: String): GemoAiResponse {
        val localRoute = resolveLocalRoute(userMessage)
        if (localRoute != null) {
            Log.d(TAG, "Respons Gemo diselesaikan lokal via route=$localRoute")
            return GemoStaticResponseRouter.responseFor(localRoute)
        }

        Log.d(TAG, "Respons Gemo diteruskan ke Gemini (${userMessage.length} karakter)")
        val modelResponse = geminiGemoService.generate(userMessage)
        if (!GemoAiResponseValidator.isValid(modelResponse)) {
            Log.w(TAG, "Respons Gemini Gemo gagal validasi akhir — fallback aman")
            return GeminiGemoService.fallbackResponse()
        }

        return modelResponse
    }

    private fun resolveLocalRoute(userMessage: String): GemoStaticResponseRoute? {
        val scopeRoute = GemoScopePrefilter.routeFor(userMessage)
        if (scopeRoute == GemoStaticResponseRoute.SPAM_OR_EMPTY) {
            return scopeRoute
        }

        return PromptInjectionPrefilter.routeFor(userMessage)
            ?: scopeRoute
    }

    companion object {
        private const val TAG = "GemoAiRepository"
    }
}
