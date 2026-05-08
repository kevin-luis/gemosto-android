package com.gemosto.feature.scan

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.R
import com.gemosto.core.designsystem.GemColors
import com.gemosto.data.pose.PoseDetector
import com.gemosto.domain.model.KneeSide
import org.koin.compose.koinInject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Layar kamera untuk pengukuran ROM.
 *
 * Hari 6: CameraX Preview + ImageAnalysis + MediaPipe PoseLandmarker
 *         + display sudut real-time di overlay (basic).
 * Hari 7: tambah landmark dot/line overlay drawing di atas preview.
 * Hari 8: 2-phase state machine + finalize hasil + save ke Firestore.
 *
 * Spec: 002-rom-scan.md.
 */
@Composable
fun RomCameraScreen(
    kneeSide: KneeSide,
    onClose: () -> Unit,
    viewModel: ScanViewModel,
) {
    LockOrientationToPortrait()

    val context = LocalContext.current
    val poseDetector: PoseDetector = koinInject()
    var permissionStatus by remember { mutableStateOf(checkInitialPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionStatus = if (granted) {
            CameraPermissionStatus.Granted
        } else {
            val activity = context.findActivity()
            val canRetry = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            if (canRetry) CameraPermissionStatus.DeniedCanRetry
            else CameraPermissionStatus.PermanentlyDenied
        }
    }

    LaunchedEffect(Unit) {
        if (permissionStatus == CameraPermissionStatus.NotRequested) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Initialize PoseDetector + start observing — disposed saat keluar Camera.
    var detectorReady by remember { mutableStateOf(false) }
    var detectorError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val initResult = poseDetector.initialize(context)
        if (initResult.isSuccess) {
            detectorReady = true
            viewModel.startObservingPose()
        } else {
            detectorError = "Gagal memuat model pose: ${initResult.exceptionOrNull()?.message ?: "tidak diketahui"}. " +
                "Pastikan file pose_landmarker_heavy.task ada di app/src/main/assets/."
        }
        onDispose {
            poseDetector.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            permissionStatus == CameraPermissionStatus.PermanentlyDenied -> {
                OpenSettingsScreen(onCancel = onClose)
            }
            permissionStatus == CameraPermissionStatus.DeniedCanRetry -> {
                PermissionRationale(
                    onRequest = { launcher.launch(Manifest.permission.CAMERA) },
                    onCancel = onClose,
                )
            }
            permissionStatus == CameraPermissionStatus.NotRequested -> {
                LoadingOverlay("Memuat kamera...")
            }
            detectorError != null -> {
                ModelErrorScreen(message = detectorError!!, onCancel = onClose)
            }
            !detectorReady -> {
                LoadingOverlay("Memuat model pose...")
            }
            else -> {
                CameraPreviewBox(
                    kneeSide = kneeSide,
                    poseDetector = poseDetector,
                    viewModel = viewModel,
                    onClose = onClose,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Permission status helpers
// ─────────────────────────────────────────────────────────────────

private enum class CameraPermissionStatus {
    NotRequested,
    Granted,
    DeniedCanRetry,
    PermanentlyDenied,
}

private fun checkInitialPermission(context: android.content.Context): CameraPermissionStatus {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    return if (granted) CameraPermissionStatus.Granted else CameraPermissionStatus.NotRequested
}

// ─────────────────────────────────────────────────────────────────
// Camera Preview + ImageAnalysis (CameraX)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CameraPreviewBox(
    kneeSide: KneeSide,
    poseDetector: PoseDetector,
    viewModel: ScanViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Background executor untuk ImageAnalysis (single-thread, dispose saat keluar)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            update = { previewView ->
                bindCameraToLifecycle(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    poseDetector = poseDetector,
                    analysisExecutor = analysisExecutor,
                )
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Landmark overlay — di-stack di atas preview, di bawah UI controls.
        PoseLandmarkOverlay(
            kneeSide = kneeSide,
            poseDetector = poseDetector,
            modifier = Modifier.fillMaxSize(),
        )

        // Top overlay: status badge + close
        TopOverlay(
            kneeSideLabel = when (kneeSide) {
                KneeSide.LEFT -> "Kiri"
                KneeSide.RIGHT -> "Kanan"
                KneeSide.BOTH -> "—"
            },
            poseQuality = state.poseQuality,
            onClose = onClose,
        )

        // Center overlay: angle indicator
        AngleIndicator(
            interiorAngleDeg = state.currentAngleDeg,
            flexionDeg = state.currentFlexionDeg,
            poseQuality = state.poseQuality,
        )

        // Bottom overlay: instruksi placeholder Hari 7-8
        BottomInstruction()
    }
}

private fun bindCameraToLifecycle(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    poseDetector: PoseDetector,
    analysisExecutor: ExecutorService,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()

            val previewUseCase = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor) { imageProxy ->
                        poseDetector.detectAsync(imageProxy)
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                imageAnalysisUseCase,
            )
        } catch (e: Exception) {
            android.util.Log.e("RomCameraScreen", "Failed to bind camera", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

// ─────────────────────────────────────────────────────────────────
// Overlays
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TopOverlay(
    kneeSideLabel: String,
    poseQuality: PoseQuality,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status badge
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = when (poseQuality) {
                                PoseQuality.Good -> Color(0xFF4ADE80)
                                PoseQuality.Low -> Color(0xFFFBBF24)
                                PoseQuality.NotDetected -> Color(0xFFF87171)
                            },
                            shape = CircleShape,
                        ),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Lutut $kneeSideLabel • ${poseQualityLabel(poseQuality)}",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Close button
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onClose),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun poseQualityLabel(q: PoseQuality): String = when (q) {
    PoseQuality.Good -> "Pose terdeteksi"
    PoseQuality.Low -> "Pose kurang jelas"
    PoseQuality.NotDetected -> "Mendeteksi pose..."
}

@Composable
private fun AngleIndicator(
    interiorAngleDeg: Float?,
    flexionDeg: Float?,
    poseQuality: PoseQuality,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val displayAngle = interiorAngleDeg
            if (displayAngle != null && poseQuality != PoseQuality.NotDetected) {
                Text(
                    text = "${"%.1f".format(displayAngle)}°",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.W600,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sudut interior lutut",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                    ),
                )
                if (flexionDeg != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Flexion ${"%.0f".format(flexionDeg)}°",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomInstruction() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Posisikan lutut Anda dari samping",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Titik hijau menandakan landmark terdeteksi jelas. Sesi 2-fase " +
                        "(extension + flexion) akan diaktifkan Hari 8.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Permission alternative screens
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LoadingOverlay(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
    }
}

@Composable
private fun ModelErrorScreen(message: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = GemColors.Danger,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Model pose tidak tersedia",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.action_close))
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequest: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = GemColors.EmeraldPrimary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.scan_camera_perm_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scan_camera_perm_rationale),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.scan_camera_perm_request_again))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.action_cancel),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(12.dp),
        )
    }
}

@Composable
private fun OpenSettingsScreen(onCancel: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = GemColors.Danger,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.scan_camera_perm_denied_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scan_camera_perm_denied_body),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.scan_camera_perm_open_settings))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.action_cancel),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(12.dp),
        )
    }
}

@Composable
private fun LockOrientationToPortrait() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val original = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = original
        }
    }
}
