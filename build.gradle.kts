// Top-level build file — root project
// Plugins di-declare di sini supaya bisa diaplikasikan di module di bawah.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.secrets) apply false
}

tasks.register("recordDebugScreenshotTest") {
    group = "verification"
    description = "Alias for the Compose screenshot plugin debug baseline update task."
    dependsOn(":app:updateDebugScreenshotTest")
}
