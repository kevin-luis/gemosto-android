package com.gemosto.data.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.gemosto.domain.rom.Point2D
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Wrapper untuk MediaPipe Pose Landmarker.
 *
 * Memuat model `pose_landmarker_heavy.task` dari assets, menjalankan deteksi
 * dalam mode LIVE_STREAM (async, callback-based), dan mengekspos hasil via
 * SharedFlow.
 *
 * **Asset wajib**: `app/src/main/assets/pose_landmarker_heavy.task`.
 * Download dari:
 *   https://storage.googleapis.com/mediapipe-models/pose_landmarker/
 *   pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task
 *
 * Detail: SETUP-MEDIAPIPE.md.
 *
 * Lifecycle:
 *  - [initialize] dipanggil dari ViewModel/Composable saat enter Camera screen
 *  - [detectAsync] dipanggil dari ImageAnalysis analyzer
 *  - [close] WAJIB dipanggil saat exit Camera screen (release native resources)
 */
class PoseDetector {

    private val _detections = MutableSharedFlow<PoseDetection>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val detections: SharedFlow<PoseDetection> = _detections.asSharedFlow()

    private var landmarker: PoseLandmarker? = null

    fun initialize(context: Context): Result<Unit> = runCatching {
        if (landmarker != null) return@runCatching   // already initialized

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(Delegate.CPU)               // GPU bisa di-V2 kalau perlu
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setNumPoses(1)
            .setResultListener(::onResult)
            .setErrorListener { e ->
                Log.e(TAG, "PoseLandmarker error", e)
            }
            .build()

        landmarker = PoseLandmarker.createFromOptions(context, options)
        Log.d(TAG, "PoseLandmarker initialized")
    }

    /**
     * Submit 1 frame untuk deteksi async. Hasil akan emit ke [detections] flow
     * via callback [onResult].
     *
     * @param imageProxy frame dari CameraX ImageAnalysis
     */
    @OptIn(ExperimentalGetImage::class)
    fun detectAsync(imageProxy: ImageProxy) {
        val active = landmarker ?: run {
            imageProxy.close()
            return
        }

        try {
            // Convert ImageProxy → rotated Bitmap → MPImage
            val bitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val rotated = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
            val mpImage = BitmapImageBuilder(rotated).build()

            // Timestamp wajib monoton increasing untuk LIVE_STREAM mode.
            val timestamp = imageProxy.imageInfo.timestamp / 1_000_000L
            active.detectAsync(mpImage, timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "detectAsync failed", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun onResult(result: PoseLandmarkerResult, mpImage: com.google.mediapipe.framework.image.MPImage) {
        val poses = result.landmarks()
        val visibility = result.worldLandmarks()    // alternative
        val timestamp = result.timestampMs()

        if (poses.isEmpty() || poses[0].isEmpty()) {
            _detections.tryEmit(PoseDetection.EMPTY.copy(timestampMs = timestamp))
            return
        }

        val firstPose = poses[0]
        val points = firstPose.map { lm -> Point2D(lm.x(), lm.y()) }
        val visibilities = firstPose.map { lm ->
            // visibility() optional di Optional<Float>
            lm.visibility().orElse(0f) ?: 0f
        }

        _detections.tryEmit(
            PoseDetection(
                landmarks = points,
                visibility = visibilities,
                timestampMs = timestamp,
            )
        )
    }

    fun close() {
        landmarker?.close()
        landmarker = null
        Log.d(TAG, "PoseLandmarker closed")
    }

    companion object {
        private const val TAG = "PoseDetector"
        private const val MODEL_ASSET = "pose_landmarker_heavy.task"
    }
}
