package com.mulkallah.aircontrole.camera

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.model.HandFrame
import com.mulkallah.aircontrole.core.model.PipelineStatus
import com.mulkallah.aircontrole.core.permissions.PermissionChecker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraHandTracker(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onHand: (HandFrame) -> Unit,
    private val onEmpty: (Long) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onCameraBound: () -> Unit = {},
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var analyzer: HandLandmarkerAnalyzer? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val framesDelivered = AtomicInteger(0)
    private val handsDelivered = AtomicInteger(0)

    val deliveredFrameCount: Int get() = framesDelivered.get()
    val deliveredHandCount: Int get() = handsDelivered.get()

    suspend fun start() {
        if (!PermissionChecker.hasCamera(context)) {
            AirControleLog.e("camera start aborted: CAMERA permission missing")
            onError(IllegalStateException(PipelineStatus.CAMERA_PERMISSION))
            return
        }
        val provider = context.getCameraProvider()
        cameraProvider = provider
        val localAnalyzer = HandLandmarkerAnalyzer(
            context = context,
            onHand = { frame ->
                framesDelivered.incrementAndGet()
                handsDelivered.incrementAndGet()
                postMain { onHand(frame) }
            },
            onEmpty = { timestamp ->
                framesDelivered.incrementAndGet()
                postMain { onEmpty(timestamp) }
            },
            onError = { error ->
                AirControleLog.e("camera/landmarker error: ${error.message}", error)
                postMain { onError(error) }
            },
        )
        analyzer = localAnalyzer
        if (!localAnalyzer.isReady) {
            AirControleLog.e("camera bound skipped: landmarker not ready")
            return
        }
        val rotation = displayRotation(context)
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(Size(480, 640))
            .setTargetRotation(rotation)
            .build()
            .also { it.setAnalyzer(cameraExecutor, localAnalyzer) }

        provider.unbindAll()
        val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            AirControleLog.i("selecting front camera rotation=$rotation")
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            AirControleLog.w("front camera missing; falling back to back camera")
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        provider.bindToLifecycle(lifecycleOwner, selector, analysis)
        AirControleLog.i("camera bound selector=${if (selector == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"}")
        onCameraBound()
    }

    fun stop() {
        AirControleLog.i("camera stop frames=${framesDelivered.get()} hands=${handsDelivered.get()}")
        try {
            cameraProvider?.unbindAll()
        } catch (error: Throwable) {
            AirControleLog.w("camera unbind failed", error)
        }
        analyzer?.close()
        analyzer = null
        cameraExecutor.shutdown()
    }

    private fun postMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}

private fun displayRotation(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display?.rotation ?: Surface.ROTATION_0
    } else {
        @Suppress("DEPRECATION")
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.rotation
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
