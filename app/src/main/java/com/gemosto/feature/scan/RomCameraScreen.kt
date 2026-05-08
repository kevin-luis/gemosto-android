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
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gemosto.R
import com.gemosto.core.designsystem.GemColors
import com.gemosto.domain.model.KneeSide

/**
 * Layar kamera untuk pengukuran ROM.
 *
 * Hari 5: BASIC PREVIEW — CameraX Preview use case + permission flow.
 * Hari 6-7: Tambah ImageAnalysis use case → MediaPipe Pose Landmarker
 *           → real-time landmark overlay + angle indicator + 2-phase state machine.
 *
 * Spec: 002-rom-scan.md section 4 — RomCameraScreen.
 */
@Composable
fun RomCameraScreen(
    kneeSide: KneeSide,
    onClose: () -> Unit,
) {
    LockOrientationToPortrait()

    val context = LocalContext.current
    var permissionStatus by remember {
        mutableStateOf(checkInitialPermission(context))
    }

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

    // Auto-launch permission request kalau belum pernah ditanya
    LaunchedEffect(Unit) {
        if (permissionStatus == CameraPermissionStatus.NotRequested) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (permissionStatus) {
            CameraPermissionStatus.Granted -> {
                CameraPreviewBox(kneeSide = kneeSide, onClose = onClose)
            }
            CameraPermissionStatus.NotRequested -> {
                LoadingOverlay()
            }
            CameraPermissionStatus.DeniedCanRetry -> {
                PermissionRationale(
                    onRequest = { launcher.launch(Manifest.permission.CAMERA) },
                    onCancel = onClose,
                )
            }
            CameraPermissionStatus.PermanentlyDenied -> {
                OpenSettingsScreen(onCancel = onClose)
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
// Camera Preview (CameraX)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CameraPreviewBox(
    kneeSide: KneeSide,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX PreviewView via AndroidView
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
                )
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Overlay top: close + status badge
        TopOverlay(
            kneeSideLabel = when (kneeSide) {
                KneeSide.LEFT -> "Kiri"
                KneeSide.RIGHT -> "Kanan"
                KneeSide.BOTH -> "—"
            },
            onClose = onClose,
        )

        // Overlay bottom — placeholder Hari 5
        BottomPlaceholder()
    }
}

private fun bindCameraToLifecycle(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val previewUseCase = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
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
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        // Status badge kiri
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterStart),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        ) {
            Text(
                text = "Lutut $kneeSideLabel",
                style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        // Close button kanan
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onClose)
                .align(Alignment.CenterEnd),
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

@Composable
private fun BottomPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Camera Preview Aktif",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Deteksi pose lutut + pengukuran sudut akan diaktifkan Hari 6-7.",
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
private fun LoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Memuat kamera...",
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        )
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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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

// ─────────────────────────────────────────────────────────────────
// Lock orientation utility
// ─────────────────────────────────────────────────────────────────

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
