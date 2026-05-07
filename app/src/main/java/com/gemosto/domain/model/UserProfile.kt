package com.gemosto.domain.model

/**
 * Sisi lutut yang bermasalah / dominan untuk pengukuran ROM.
 *
 * BOTH digunakan saat user punya issue di kedua lutut — di flow scan
 * akan diminta pilih salah satu (LEFT atau RIGHT) per sesi.
 */
enum class KneeSide { LEFT, RIGHT, BOTH }

/**
 * Level aktivitas user — dipakai sebagai modifier di ExerciseRuleEngine.
 */
enum class ActivityLevel { LOW, MODERATE, ACTIVE }

/**
 * Profil user. Disimpan di Firestore path `users/{uid}/profile/data`.
 *
 * Field `disclaimerAcceptedAt` adalah epoch ms saat user menyetujui
 * disclaimer medis di onboarding — wajib > 0 sebelum profil dianggap valid.
 */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val age: Int,
    val affectedKnee: KneeSide,
    val activityLevel: ActivityLevel,
    val disclaimerAcceptedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isValid: Boolean
        get() = uid.isNotBlank()
            && name.isNotBlank()
            && age in 18..100
            && disclaimerAcceptedAt > 0L
}
