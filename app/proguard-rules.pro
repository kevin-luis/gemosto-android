# Proguard rules untuk MVP Gemosto

# Keep MediaPipe internals
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }

# Keep generative AI (Gemini SDK)
-keep class com.google.ai.client.generativeai.** { *; }

# Firebase (sudah di-handle oleh Firebase plugin, tapi eksplisit)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Kotlin metadata
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Domain models (akan disimpan di Firestore — perlu reflective access)
-keep class com.gemosto.domain.model.** { *; }
