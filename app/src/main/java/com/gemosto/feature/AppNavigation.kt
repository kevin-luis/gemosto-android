package com.gemosto.feature

/**
 * Route names untuk Compose Navigation.
 *
 * Pakai const string supaya simple. Nanti bisa migrasi ke type-safe
 * navigation (Kotlin Serialization) di V2 kalau perlu.
 */
object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val PROFILE_SETUP = "onboarding/profile"
    const val HOME = "home"
}
