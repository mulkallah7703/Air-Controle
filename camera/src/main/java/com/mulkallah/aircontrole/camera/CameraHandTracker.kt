package com.mulkallah.aircontrole.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.mulkallah.aircontrole.core.model.HandFrame
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraHandTracker(
    private val context: Context,
    private val onHand: (HandFrame) -> Unit,
    private val onEmpty: (Long) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var analyzer: HandLandmarkerAnalyzer? = null
    private var cameraProvider: ProcessCameraProvider? = null

    suspend fun start() {
        lifecycleOwner.start()
        val provider = context.getCameraProvider()
        cameraProvider = provider
        val localAnalyzer = HandLandmarkerAnalyzer(context, onHand, onEmpty, onError)
        analyzer = localAnalyzer
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { it.setAnalyzer(cameraExecutor, localAnalyzer) }

        provider.unbindAll()
        val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        provider.bindToLifecycle(lifecycleOwner, selector, analysis)
    }

    fun stop() {
        cameraProvider?.unbindAll()
        analyzer?.close()
        analyzer = null
        lifecycleOwner.stop()
        cameraExecutor.shutdown()
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (error: Throwable) {
                    continuation.resumeWithException(error)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }
