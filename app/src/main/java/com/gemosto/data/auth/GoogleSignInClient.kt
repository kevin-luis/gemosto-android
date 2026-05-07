package com.gemosto.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Wrapper untuk Google Sign-In via Credential Manager API.
 *
 * Web Client ID di-resolve dari resource `R.string.default_web_client_id`
 * yang auto-generated oleh `google-services` plugin dari `google-services.json`.
 *
 * **PENTING — Pemilihan API:**
 * Kita pakai `GetSignInWithGoogleOption` (bukan `GetGoogleIdOption`).
 * - `GetSignInWithGoogleOption` → untuk **explicit user click pada button**
 *   "Sign in with Google" — selalu menampilkan akun picker.
 * - `GetGoogleIdOption` → untuk **silent / one-tap** sign-in (auto-show kalau
 *   sudah pernah authorize). Sering gagal di first-time login dengan error
 *   misterius.
 *
 * Reference: https://developer.android.com/identity/sign-in/credential-manager-siwg
 */
class GoogleSignInClient(
    private val webClientId: String,
) {

    sealed class Result {
        data class Success(val idToken: String) : Result()
        data object Canceled : Result()
        /**
         * @param message Pesan untuk ditampilkan ke user (Bahasa Indonesia)
         * @param errorType String identifier untuk debug — biasanya class name
         *                  exception atau type field dari GetCredentialException
         * @param cause Exception aslinya — sudah dilog ke Logcat tag "GoogleSignIn"
         */
        data class Failed(
            val message: String,
            val errorType: String,
            val cause: Throwable? = null,
        ) : Result()
    }

    suspend fun signIn(activityContext: Context): Result {
        // ─── Validasi config sebelum panggil API ────────────────────
        if (webClientId.isBlank()) {
            Log.e(TAG, "webClientId KOSONG — google-services.json mungkin missing")
            return Result.Failed(
                message = "Web Client ID kosong. Periksa google-services.json + " +
                          "Auth Google sudah enable di Firebase Console.",
                errorType = "ConfigError",
            )
        }
        Log.d(TAG, "Starting sign-in. webClientId=${webClientId.take(20)}…(${webClientId.length} chars)")

        val credentialManager = CredentialManager.create(activityContext)

        // GetSignInWithGoogleOption — explicit Sign-in flow (menampilkan picker)
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val response = credentialManager.getCredential(activityContext, request)
            parseResponse(response)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled sign-in")
            Result.Canceled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "NoCredentialException", e)
            Result.Failed(
                message = "Tidak ada akun Google di device. Tambahkan akun lewat Pengaturan Android > Akun.",
                errorType = "NoCredential",
                cause = e,
            )
        } catch (e: GetCredentialException) {
            // type field penting — bisa "android.credentials.GetCredentialException.TYPE_USER_CANCELED",
            // "TYPE_NO_CREDENTIAL", dll. Biasanya juga embed message dari GMS.
            Log.e(TAG, "GetCredentialException: type=${e.type} msg=${e.message}", e)
            val readable = when {
                e.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true -> {
                    "DEVELOPER_ERROR — SHA-1 debug Anda belum terdaftar di Firebase Console " +
                    "atau Web Client ID tidak match. Cek: SETUP-HARI-1.md langkah 2."
                }
                e.message?.contains("Caller has been temporarily blocked", ignoreCase = true) == true -> {
                    "Akses temporary diblokir Google. Tunggu 5-10 menit lalu coba lagi."
                }
                e.type.contains("INTERRUPTED", ignoreCase = true) -> {
                    "Login terinterupsi. Coba lagi."
                }
                else -> "Login gagal: ${e.message ?: e.type}"
            }
            Result.Failed(message = readable, errorType = e.type, cause = e)
        } catch (e: UnsupportedOperationException) {
            // Khas error "gRPC BrowseService" saat Play Store belum login
            Log.e(TAG, "UnsupportedOperationException — Play Store mungkin belum login", e)
            Result.Failed(
                message = "Pastikan Anda sudah login ke Google Play Store di device ini.",
                errorType = "UnsupportedOperation",
                cause = e,
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            Result.Failed(
                message = "Error tak terduga: ${e.javaClass.simpleName} — ${e.message ?: "—"}",
                errorType = e.javaClass.simpleName,
                cause = e,
            )
        }
    }

    private fun parseResponse(response: GetCredentialResponse): Result {
        val credential = response.credential
        return when {
            credential is CustomCredential
                && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    Log.d(TAG, "Sign-in success. email=${googleCred.id}")
                    Result.Success(googleCred.idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Failed to parse Google ID token", e)
                    Result.Failed(
                        message = "Token Google tidak dapat dibaca.",
                        errorType = "TokenParse",
                        cause = e,
                    )
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential type: ${credential.type}")
                Result.Failed(
                    message = "Tipe credential tidak dikenal: ${credential.type}",
                    errorType = "UnknownCredentialType",
                )
            }
        }
    }

    companion object {
        private const val TAG = "GoogleSignIn"
    }
}
