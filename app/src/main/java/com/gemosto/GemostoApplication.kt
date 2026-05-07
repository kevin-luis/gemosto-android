package com.gemosto

import android.app.Application
import com.gemosto.core.di.appModule
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application root.
 *
 * Tugas:
 *  1. Initialize Firebase
 *  2. Start Koin DI dengan single appModule
 *
 * Catatan: Firebase init sebenarnya auto-trigger oleh google-services plugin
 * via ContentProvider — pemanggilan eksplisit di sini hanya defensive.
 */
class GemostoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("Gemosto", "API key configured: ${BuildConfig.GEMINI_API_KEY.take(8)}...")
        /*
        // Defensive Firebase init (idempotent kalau sudah auto-init)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        */

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@GemostoApplication)
            modules(appModule)
        }
    }
}
