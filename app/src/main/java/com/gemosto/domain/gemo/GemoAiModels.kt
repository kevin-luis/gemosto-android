package com.gemosto.domain.gemo

/**
 * Status cakupan pertanyaan user terhadap jalur edukasi Gemo AI.
 */
enum class ScopeStatus {
    IN_SCOPE,
    RELATED_SCOPE,
    OUT_OF_SCOPE,
}

/**
 * Tingkat risiko keselamatan untuk respons Gemo AI.
 */
enum class RiskLevel {
    LOW,
    CAUTION,
    URGENT,
}

/**
 * Jenis respons final yang boleh ditampilkan aplikasi.
 */
enum class ResponseType {
    EDUCATION,
    REFUSAL,
    ESCALATION,
}

/**
 * Aksi lanjutan yang disarankan setelah respons diberikan.
 */
enum class RecommendedAction {
    NONE,
    CONSULT_DOCTOR,
    SEEK_URGENT_CARE,
}

/**
 * Kontrak respons terstruktur Gemo AI yang dipakai lintas layer.
 */
data class GemoAiResponse(
    val scopeStatus: ScopeStatus,
    val riskLevel: RiskLevel,
    val responseType: ResponseType,
    val answer: String,
    val disclaimer: String,
    val recommendedAction: RecommendedAction,
)

/**
 * Pertanyaan awal statis yang tampil saat sesi baru dibuka.
 */
data class SuggestedQuestion(
    val text: String,
)

/**
 * Disclaimer baku yang dibekukan oleh prompt package Gemo AI.
 */
object GemoAiDisclaimers {
    const val DEFAULT = "Gemo bisa keliru, dan informasi ini bukan pengganti pemeriksaan dokter."
    const val DIAGNOSIS_OR_MEDICATION =
        "Gemo bisa keliru dan tidak dapat menggantikan pemeriksaan atau anjuran dokter."
    const val URGENT =
        "Gemo bisa keliru, tetapi gejala seperti ini perlu dinilai tenaga medis segera."
}
