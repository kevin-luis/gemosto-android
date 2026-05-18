import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.secrets)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { input ->
            load(input)
        }
    }
}

fun localBooleanProperty(
    name: String,
    defaultValue: Boolean,
): String {
    return localProperties
        .getProperty(name, defaultValue.toString())
        .toBooleanStrictOrNull()
        ?.toString()
        ?: defaultValue.toString()
}

android {
    namespace = "com.gemosto"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.gemosto"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0-mvp"
        buildConfigField("String", "GEMINI_MODEL", "\"gemini-2.5-flash\"")
        buildConfigField("String", "GEMINI_BACKUP_MODEL", "\"gemini-2.5-flash-lite\"")
        buildConfigField("boolean", "GEMO_USE_FAKE_PROVIDER", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Lock orientation di camera screen — kita lock per-Activity, bukan global
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            buildConfigField(
                "boolean",
                "GEMO_USE_FAKE_PROVIDER",
                localBooleanProperty(
                    name = "GEMO_USE_FAKE_PROVIDER",
                    defaultValue = false,
                ),
            )
        }
        release {
            isMinifyEnabled = false  // di-disable dulu untuk MVP, enable saat final build
            buildConfigField("boolean", "GEMO_USE_FAKE_PROVIDER", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")  // MVP: pakai debug signing
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Asset MediaPipe model (.task) jangan di-compress
        // (sudah default tapi eksplisit lebih aman)
    }

    androidResources {
        noCompress += listOf("task", "tflite")
    }
}

// Secrets Gradle Plugin config
// Plugin akan baca dari local.properties → BuildConfig.GEMINI_API_KEY
secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
    // Field yang TIDAK di-inject ke BuildConfig
    ignoreList.add("sdk.*")
    ignoreList.add("keyToIgnore.*")
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)

    // Compose (BOM-managed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // CameraX
    implementation(libs.bundles.camerax)

    // Credentials (Google Sign-In)
    implementation(libs.bundles.credentials)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Firebase (BOM-managed)
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // MediaPipe Pose Landmarker
    implementation(libs.mediapipe.tasks.vision)

    // Generative AI (Gemini)
    implementation(libs.generative.ai)

    implementation(libs.compose.markdown)

    // Koin
    implementation(libs.bundles.koin)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
