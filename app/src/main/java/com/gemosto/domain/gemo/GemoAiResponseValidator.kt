package com.gemosto.domain.gemo

/**
 * Isu validasi deterministik yang bisa diperiksa setelah parsing response model.
 */
enum class GemoAiResponseValidationIssue {
    BLANK_ANSWER,
    BLANK_DISCLAIMER,
    OUT_OF_SCOPE_REQUIRES_REFUSAL,
    URGENT_REQUIRES_ESCALATION,
    URGENT_REQUIRES_SEEK_URGENT_CARE,
}

/**
 * Validator post-parse untuk kontrak respons Gemo AI.
 *
 * Validasi enum invalid seperti `risk_level = medium` akan terjadi saat parsing JSON
 * di service layer nanti, karena setelah masuk domain semua enum sudah bertipe kuat.
 */
object GemoAiResponseValidator {

    fun isValid(response: GemoAiResponse): Boolean = issuesFor(response).isEmpty()

    fun issuesFor(response: GemoAiResponse): Set<GemoAiResponseValidationIssue> {
        return buildSet {
            if (response.answer.isBlank()) {
                add(GemoAiResponseValidationIssue.BLANK_ANSWER)
            }

            if (response.disclaimer.isBlank()) {
                add(GemoAiResponseValidationIssue.BLANK_DISCLAIMER)
            }

            if (
                response.scopeStatus == ScopeStatus.OUT_OF_SCOPE &&
                response.responseType != ResponseType.REFUSAL
            ) {
                add(GemoAiResponseValidationIssue.OUT_OF_SCOPE_REQUIRES_REFUSAL)
            }

            if (
                response.riskLevel == RiskLevel.URGENT &&
                response.responseType != ResponseType.ESCALATION
            ) {
                add(GemoAiResponseValidationIssue.URGENT_REQUIRES_ESCALATION)
            }

            if (
                response.riskLevel == RiskLevel.URGENT &&
                response.recommendedAction != RecommendedAction.SEEK_URGENT_CARE
            ) {
                add(GemoAiResponseValidationIssue.URGENT_REQUIRES_SEEK_URGENT_CARE)
            }
        }
    }
}
