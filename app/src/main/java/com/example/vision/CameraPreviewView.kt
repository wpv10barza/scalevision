package com.example.vision

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.ProcessCameraProvider
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    torchEnabled: Boolean = false,
    zoomRatio: Float = 0f,
    onBitmapProviderReady: ((() -> Bitmap?)) -> Unit = {},
    onCameraReady: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(torchEnabled, camera) {
        try {
            camera?.cameraControl?.enableTorch(torchEnabled)
        } catch (_: Exception) {}
    }

    LaunchedEffect(zoomRatio, camera) {
        try {
            camera?.cameraControl?.setLinearZoom(zoomRatio.coerceIn(0f, 1f))
        } catch (_: Exception) {}
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewViewInstance = this
                onBitmapProviderReady { this.bitmap }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also { it.surfaceProvider = surfaceProvider }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                        onCameraReady(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onCameraReady(false)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier
            .fillMaxSize()
            .pointerInput(previewViewInstance, camera) {
                detectTapGestures { offset ->
                    val pv = previewViewInstance ?: return@detectTapGestures
                    val cam = camera ?: return@detectTapGestures
                    try {
                        val factory = SurfaceOrientedMeteringPointFactory(
                            pv.width.toFloat(),
                            pv.height.toFloat()
                        )
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cam.cameraControl.startFocusAndMetering(action)
                    } catch (_: Exception) {}
                }
            }
    )
}
