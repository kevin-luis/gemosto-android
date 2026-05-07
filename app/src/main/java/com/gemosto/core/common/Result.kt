package com.gemosto.core.common

/**
 * Hasil operasi yang bisa sukses atau gagal.
 *
 * Sengaja TIDAK pakai `kotlin.Result` karena:
 *  - Tidak bisa di-return dari suspend function langsung tanpa `@OptIn(ExperimentalApi::class)`
 *  - Lebih sulit untuk loading state representation
 *
 * Pakai sealed class custom supaya bisa handle Loading state untuk UI.
 */
sealed interface GemResult<out T> {
    data object Loading : GemResult<Nothing>
    data class Success<T>(val data: T) : GemResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : GemResult<Nothing>
}

/**
 * Helper untuk wrap suspend block ke GemResult.
 */
suspend inline fun <T> safeCall(crossinline block: suspend () -> T): GemResult<T> {
    return try {
        GemResult.Success(block())
    } catch (e: Throwable) {
        GemResult.Error(message = e.message ?: "Unknown error", cause = e)
    }
}

/**
 * Map success data, leave Error/Loading as-is.
 */
inline fun <T, R> GemResult<T>.map(transform: (T) -> R): GemResult<R> = when (this) {
    is GemResult.Success -> GemResult.Success(transform(data))
    is GemResult.Error -> this
    GemResult.Loading -> GemResult.Loading
}
