package com.gemosto.data.llm

import android.util.Log
import com.gemosto.BuildConfig
import com.gemosto.domain.exercise.Exercise
import com.gemosto.domain.model.ExerciseLevel
import com.gemosto.domain.model.ExerciseNarrative
import com.gemosto.domain.model.KneeSide
import com.gemosto.domain.model.NarrativeSource
import com.gemosto.domain.model.RomCategory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * Service untuk generate narrative exercise program via Gemini AI.
 *
 * **Kontrak penting (argumen safety skripsi):**
 * - Service ini HANYA isi 3 field text: intro, rationale, weeklyMotivation
 * - TIDAK menerima/menghasilkan parameter klinis (gerakan, sets, reps)
 * - Output dipaksa JSON via `responseMimeType = "application/json"` + schema
 * - Kalau gagal/timeout/parse error → fallback statis built-in (UX tidak block)
 *
 * Spec: 003-exercise-recommendation.md section 7.
 */
class GeminiNarrativeService {

    /**
     * Generate narrative untuk program. Selalu return [ExerciseNarrative] —
     * fallback statis dipakai kalau Gemini call gagal.
     */
    suspend fun generate(input: NarrativeInput): ExerciseNarrative {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "GEMINI_API_KEY kosong — pakai fallback statis")
            return fallbackNarrative(input)
        }

        return try {
            withTimeout(TIMEOUT_MS) {
                callGemini(input, apiKey)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Gemini timeout setelah ${TIMEOUT_MS}ms — fallback")
            fallbackNarrative(input)
        } catch (e: Throwable) {
            Log.e(TAG, "Gemini call failed — fallback", e)
            fallbackNarrative(input)
        }
    }

    private suspend fun callGemini(input: NarrativeInput, apiKey: String): ExerciseNarrative {
        val model = GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = NARRATIVE_SCHEMA
                temperature = 0.7f
                maxOutputTokens = 500
            },
            systemInstruction = content { text(SYSTEM_INSTRUCTION) },
        )

        val prompt = buildPrompt(input)
        Log.d(TAG, "Gemini prompt:\n$prompt")

        val response = retryOnce {
            model.generateContent(prompt)
        }

        val rawText = response.text
            ?: throw IllegalStateException("Gemini response.text null")

        val parsed = parseNarrativeJson(rawText)
        Log.d(TAG, "Gemini narrative OK: ${parsed.intro.take(40)}...")
        return parsed
    }

    /**
     * Retry sekali untuk error transient (429/503/network blip).
     */
    private suspend fun <T> retryOnce(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: Throwable) {
            val msg = e.message.orEmpty()
            val retryable = "429" in msg || "503" in msg || "RESOURCE_EXHAUSTED" in msg
            if (!retryable) throw e
            Log.w(TAG, "Retryable error: $msg — retry sekali setelah 2s")
            delay(2000)
            block()
        }
    }

    private fun parseNarrativeJson(rawText: String): ExerciseNarrative {
        val json = JSONObject(rawText)
        val intro = json.optString("intro").trim()
        val rationale = json.optString("rationale").trim()
        val motiv = json.optString("weeklyMotivation").trim()
        require(intro.isNotEmpty() && rationale.isNotEmpty() && motiv.isNotEmpty()) {
            "Field narrative kosong: intro=${intro.length} rat=${rationale.length} motiv=${motiv.length}"
        }
        return ExerciseNarrative(
            intro = intro,
            rationale = rationale,
            weeklyMotivation = motiv,
            source = NarrativeSource.GEMINI,
        )
    }

    private fun buildPrompt(input: NarrativeInput): String {
        val exerciseList = input.exercises.joinToString { it.name }
        val kneeLabel = when (input.kneeSide) {
            KneeSide.LEFT -> "kiri"
            KneeSide.RIGHT -> "kanan"
            KneeSide.BOTH -> "kanan/kiri"
        }
        return """
            Buatkan narasi untuk program latihan berikut:
            - Kategori ROM user: ${input.romCategory.displayLabel} (${input.romCategory.name})
            - Umur: ${input.age} tahun
            - Sisi lutut yang dilatih: $kneeLabel
            - Level program: ${input.level.displayLabel} (${input.level.name})
            - Daftar latihan: $exerciseList
            - Durasi: ${input.durationWeeks} minggu, ${input.frequencyPerWeek}

            Hasilkan JSON dengan field:
            - intro: sapaan personal hangat, max 2 kalimat
            - rationale: alasan medis sederhana mengapa program ini cocok, max 3 kalimat
            - weeklyMotivation: 1 kalimat motivasi mingguan
        """.trimIndent()
    }

    /**
     * Fallback statis kalau Gemini gagal — pesan generic per level.
     * Tidak ID-token aware, tapi cukup untuk UX tidak block.
     */
    private fun fallbackNarrative(input: NarrativeInput): ExerciseNarrative {
        val intro = "Berikut program latihan yang disusun untuk Anda."
        val rationale = when (input.level) {
            ExerciseLevel.GENTLE ->
                "Kategori ROM ${input.romCategory.displayLabel} menunjukkan keterbatasan ringan-sedang. " +
                    "Program ini fokus pada gerakan low-impact untuk menjaga sendi tetap aktif " +
                    "tanpa memperberat kondisi."
            ExerciseLevel.STRENGTHENING ->
                "ROM Anda memungkinkan latihan penguatan terkontrol. " +
                    "Program ini fokus membangun kekuatan otot di sekitar lutut secara bertahap " +
                    "untuk mendukung stabilitas sendi."
            ExerciseLevel.FUNCTIONAL ->
                "ROM Anda dalam kategori baik. Program ini fokus pada gerakan fungsional " +
                    "untuk mempertahankan kebugaran sendi dan kekuatan saat aktivitas sehari-hari."
            ExerciseLevel.BLOCK ->
                "Kondisi Anda saat ini membutuhkan evaluasi tenaga medis " +
                    "sebelum memulai latihan rumahan."
        }
        val motiv = "Konsistensi adalah kunci — 30 menit setiap sesi dapat membuat perbedaan."
        return ExerciseNarrative(intro, rationale, motiv, NarrativeSource.FALLBACK_STATIC)
    }

    companion object {
        private const val TAG = "GeminiNarrative"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val TIMEOUT_MS = 8_000L

        private val SYSTEM_INSTRUCTION = """
            Anda adalah Gemosto, asisten edukasi kesehatan lutut.
            Aturan WAJIB:
            - Bahasa Indonesia sederhana, sapa user dengan "Anda"
            - TIDAK mendiagnosa, TIDAK meresepkan obat
            - TIDAK mengubah parameter latihan (sets, reps, gerakan)
            - Output WAJIB JSON valid sesuai schema yang diberikan
            - Tone hangat, empatik, presisi medis tapi tidak overclaim
        """.trimIndent()

        private val NARRATIVE_SCHEMA = Schema.obj(
            name = "ExerciseNarrative",
            description = "Narasi program latihan untuk user OA lutut",
            Schema.str(
                name = "intro",
                description = "Sapaan personal hangat, max 2 kalimat. Bahasa Indonesia.",
            ),
            Schema.str(
                name = "rationale",
                description = "Alasan medis sederhana mengapa program ini cocok, max 3 kalimat.",
            ),
            Schema.str(
                name = "weeklyMotivation",
                description = "Satu kalimat motivasi mingguan.",
            ),
        )
    }
}

/**
 * Input untuk [GeminiNarrativeService.generate].
 *
 * Snapshot dari output rule engine + profile user — Gemini hanya BACA,
 * tidak modifikasi field apa pun.
 */
data class NarrativeInput(
    val romCategory: RomCategory,
    val age: Int,
    val kneeSide: KneeSide,
    val level: ExerciseLevel,
    val exercises: List<Exercise>,
    val durationWeeks: Int,
    val frequencyPerWeek: String,
)
