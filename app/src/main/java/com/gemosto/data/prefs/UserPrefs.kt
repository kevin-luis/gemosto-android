package com.gemosto.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences wrapper untuk non-sensitive prefs.
 *
 * Yang TIDAK boleh disimpan di sini:
 *  - Token autentikasi (Firebase Auth handle sendiri)
 *  - Data medis (semua di Firestore)
 *  - PII (nama, email — di Firestore profile)
 *
 * Yang boleh:
 *  - Onboarding flag
 *  - Pain warning dismiss timestamp
 *  - UI preferences (V2: language, font scale)
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gemosto_prefs",
)

class UserPrefs(private val context: Context) {

    /**
     * `true` kalau user sudah complete profile setup (Firestore profile saved
     * + DataStore flag set). Dipakai sebagai shortcut untuk skip routing query.
     *
     * Note: Source-of-truth tetap Firestore profile. Flag ini hanya cache
     * untuk performance — kalau Firestore profile hilang, AppViewModel
     * tetap akan deteksi dan redirect ke ProfileSetup.
     */
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = value
        }
    }

    /**
     * Timestamp (epoch ms) terakhir user dismiss banner pain warning di Home.
     * 0 = belum pernah dismiss.
     */
    val painWarningDismissedAt: Flow<Long> = context.dataStore.data
        .map { it[KEY_PAIN_WARNING_DISMISSED_AT] ?: 0L }

    suspend fun setPainWarningDismissedAt(timestampMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PAIN_WARNING_DISMISSED_AT] = timestampMs
        }
    }

    /**
     * Clear semua prefs — dipanggil saat sign out atau hapus akun.
     */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun clearOnboardingAndWarnings() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ONBOARDING_COMPLETED)
            prefs.remove(KEY_PAIN_WARNING_DISMISSED_AT)
            // language tidak di-clear (UX preference) jika ada di masa depan
        }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_PAIN_WARNING_DISMISSED_AT = longPreferencesKey("pain_warning_dismissed_at")
    }
}
