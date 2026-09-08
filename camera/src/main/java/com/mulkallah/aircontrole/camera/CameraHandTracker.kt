package com.mulkallah.aircontrole.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.model.HandFrame
import com.mulkallah.aircontrole.core.model.PipelineStatus
import com.mulkallah.aircontrole.core.permissions.PermissionChecker
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class CameraHandTracker(
    context: Context,
    private val onHand: (HandFrame) -> Unit,
    private val onEmpty: (Long) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onCameraBound: () -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var analyzer: HandLandmarkerAnalyzer? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var dummyPreview: HeldPreviewSurface? = null
    private val framesDelivered = AtomicInteger(0)
    private val handsDelivered = AtomicInteger(0)
    @Volatile
    var isBound: Boolean = false
        private set

    val deliveredFrameCount: Int get() = framesDelivered.get()
    val deliveredHandCount: Int get() = handsDelivered.get()
    val analyzedFrameCount: Int get() = analyzer?.analyzedFrameCount ?: 0

    suspend fun start() {
        if (!PermissionChecker.hasCamera(appContext)) {
            AirControleLog.e("camera start aborted: CAMERA permission missing")
            onError(IllegalStateException(PipelineStatus.CAMERA_PERMISSION))
            return
        }
        try {
            lifecycleOwner.start()
        } catch (error: Throwable) {
            AirControleLog.e("camera lifecycle start failed: ${error.message}", error)
            onError(error)
            return
        }
        val provider = try {
            appContext.getCameraProvider()
        } catch (error: Throwable) {
            AirControleLog.e(
                "ProcessCameraProvider.getInstance failed ${error.javaClass.name}: ${error.message}",
                error,
            )
            onError(IllegalStateException(PipelineStatus.CAMERA_START_FAILED, error))
            return
        }
        cameraProvider = provider
        logAvailableCameras(provider)

        val localAnalyzer = analyzer ?: HandLandmarkerAnalyzer(
            context = appContext,
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
                AirControleLog.e(
                    "camera/landmarker error ${error.javaClass.name}: ${error.message}",
                    error,
                )
                postMain { onError(error) }
            },
        )
        analyzer = localAnalyzer
        if (!localAnalyzer.isReady) {
            AirControleLog.e("camera bound skipped: landmarker not ready")
            return
        }
        bindWithRetries(provider, localAnalyzer)
    }

    suspend fun rebind() {
        val provider = cameraProvider ?: appContext.getCameraProvider().also { cameraProvider = it }
        val localAnalyzer = analyzer
        if (localAnalyzer == null || !localAnalyzer.isReady) {
            AirControleLog.e("camera rebind aborted: analyzer not ready")
            return
        }
        AirControleLog.i("camera rebind requested lifecycle=${lifecycleOwner.lifecycle.currentState}")
        try {
            lifecycleOwner.start()
        } catch (error: Throwable) {
            AirControleLog.e("camera lifecycle restart failed: ${error.message}", error)
        }
        bindWithRetries(provider, localAnalyzer)
    }

    fun stop() {
        AirControleLog.i(
            "camera stop frames=${framesDelivered.get()} hands=${handsDelivered.get()} " +
                "analyzed=${analyzedFrameCount} bound=$isBound",
        )
        isBound = false
        try {
            cameraProvider?.unbindAll()
        } catch (error: Throwable) {
            AirControleLog.w("camera unbind failed ${error.javaClass.name}: ${error.message}", error)
        }
        dummyPreview?.close()
        dummyPreview = null
        analyzer?.close()
        analyzer = null
        try {
            lifecycleOwner.stop()
        } catch (error: Throwable) {
            AirControleLog.w("camera lifecycle stop failed: ${error.message}", error)
        }
        cameraExecutor.shutdown()
    }

    private suspend fun bindWithRetries(
        provider: ProcessCameraProvider,
        localAnalyzer: HandLandmarkerAnalyzer,
    ) {
        val attempts = bindAttempts(provider)
        var lastError: Throwable? = null
        for ((index, attempt) in attempts.withIndex()) {
            try {
                bindOnce(provider, localAnalyzer, attempt)
                isBound = true
                AirControleLog.i(
                    "camera bound selector=${attempt.selectorLabel} rgba=${attempt.rgba} " +
                        "preview=${attempt.withPreview} lifecycle=${lifecycleOwner.lifecycle.currentState}",
                )
                onCameraBound()
                return
            } catch (error: Throwable) {
                lastError = error
                isBound = false
                AirControleLog.e(
                    "camera bind failed attempt=${index + 1}/${attempts.size} " +
                        "selector=${attempt.selectorLabel} rgba=${attempt.rgba} " +
                        "preview=${attempt.withPreview} " +
                        "lifecycle=${lifecycleOwner.lifecycle.currentState} " +
                        "${error.javaClass.name}: ${error.message}",
                    error,
                )
                safeUnbind(provider)
                if (index < attempts.lastIndex) {
                    val backoffMs = 200L shl index
                    AirControleLog.i("camera bind retry in ${backoffMs}ms")
                    delay(backoffMs)
                }
            }
        }
        onError(IllegalStateException(PipelineStatus.CAMERA_START_FAILED, lastError))
    }

    private suspend fun bindOnce(
        provider: ProcessCameraProvider,
        localAnalyzer: HandLandmarkerAnalyzer,
        attempt: BindAttempt,
    ) = withContext(Dispatchers.Main.immediate) {
        localAnalyzer.resetBusy()
        val rotation = displayRotation(appContext)
        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setResolutionSelector(conservativeResolution())
        if (attempt.rgba) {
            analysisBuilder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        }
        analysisBuilder.setOutputImageRotationEnabled(true)
        val analysis = analysisBuilder.build().also { useCase ->
            useCase.setAnalyzer(ensureExecutor(), localAnalyzer)
        }

        safeUnbind(provider)

        val preview = if (attempt.withPreview) {
            val held = HeldPreviewSurface(ContextCompat.getMainExecutor(appContext))
            dummyPreview = held
            Preview.Builder()
                .setTargetRotation(rotation)
                .setResolutionSelector(conservativeResolution())
                .build()
                .also { it.setSurfaceProvider(held.provider) }
        } else {
            null
        }

        AirControleLog.i(
            "camera bind trying selector=${attempt.selectorLabel} rgba=${attempt.rgba} " +
                "preview=${attempt.withPreview} rotation=$rotation target=640x480 " +
                "lifecycle=${lifecycleOwner.lifecycle.currentState}",
        )
        if (preview == null) {
            provider.bindToLifecycle(lifecycleOwner, attempt.selector, analysis)
        } else {
            provider.bindToLifecycle(lifecycleOwner, attempt.selector, analysis, preview)
        }
    }

    private fun bindAttempts(provider: ProcessCameraProvider): List<BindAttempt> {
        val frontAvailable = try {
            provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        } catch (error: Throwable) {
            AirControleLog.w("hasCamera(FRONT) failed: ${error.message}", error)
            false
        }
        val backAvailable = try {
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (error: Throwable) {
            AirControleLog.w("hasCamera(BACK) failed: ${error.message}", error)
            false
        }
        AirControleLog.i("camera selectors front=$frontAvailable back=$backAvailable")
        val attempts = mutableListOf<BindAttempt>()
        if (frontAvailable) {
            attempts += BindAttempt("front", CameraSelector.DEFAULT_FRONT_CAMERA, rgba = false, withPreview = true)
            attempts += BindAttempt("front", CameraSelector.DEFAULT_FRONT_CAMERA, rgba = true, withPreview = true)
            attempts += BindAttempt("front", CameraSelector.DEFAULT_FRONT_CAMERA, rgba = false, withPreview = false)
            attempts += BindAttempt("front", CameraSelector.DEFAULT_FRONT_CAMERA, rgba = true, withPreview = false)
        }
        if (backAvailable) {
            attempts += BindAttempt("back", CameraSelector.DEFAULT_BACK_CAMERA, rgba = false, withPreview = true)
            attempts += BindAttempt("back", CameraSelector.DEFAULT_BACK_CAMERA, rgba = false, withPreview = false)
        }
        if (attempts.isEmpty()) {
            AirControleLog.e("no camera selectors available; trying DEFAULT_FRONT_CAMERA anyway")
            attempts += BindAttempt("front-fallback", CameraSelector.DEFAULT_FRONT_CAMERA, rgba = false, withPreview = true)
        }
        return attempts
    }

    private fun safeUnbind(provider: ProcessCameraProvider) {
        try {
            provider.unbindAll()
        } catch (error: Throwable) {
            AirControleLog.w("unbindAll failed ${error.javaClass.name}: ${error.message}", error)
        }
        dummyPreview?.close()
        dummyPreview = null
    }

    private fun ensureExecutor(): ExecutorService {
        if (cameraExecutor.isShutdown || cameraExecutor.isTerminated) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        return cameraExecutor
    }

    private fun postMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}

private data class BindAttempt(
    val selectorLabel: String,
    val selector: CameraSelector,
    val rgba: Boolean,
    val withPreview: Boolean,
)

private fun conservativeResolution(): ResolutionSelector {
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(640, 480),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
            ),
        )
        .build()
}

private fun logAvailableCameras(provider: ProcessCameraProvider) {
    try {
        val infos = provider.availableCameraInfos
        AirControleLog.i("ProcessCameraProvider cameras=${infos.size}")
        infos.forEachIndexed { index, info ->
            AirControleLog.i("camera[$index] facing=${info.lensFacing}")
        }
    } catch (error: Throwable) {
        AirControleLog.w("availableCameraInfos failed: ${error.message}", error)
    }
}

private fun displayRotation(context: Context): Int {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.rotation
        }
    } catch (error: Throwable) {
        AirControleLog.w("display rotation failed: ${error.message}", error)
        Surface.ROTATION_0
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val app = applicationContext
        AirControleLog.i("ProcessCameraProvider.getInstance ctx=${app.javaClass.name}")
        val future = ProcessCameraProvider.getInstance(app)
        future.addListener(
            {
                try {
                    val provider = future.get()
                    AirControleLog.i("ProcessCameraProvider ready")
                    if (continuation.isActive) continuation.resume(provider)
                } catch (error: Throwable) {
                    AirControleLog.e(
                        "ProcessCameraProvider future failed ${error.javaClass.name}: ${error.message}",
                        error,
                    )
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            },
            ContextCompat.getMainExecutor(app),
        )
    }

/**
 * Samsung Camera2 often fails ImageAnalysis-only binds. A held dummy Preview
 * surface satisfies the combination without showing a viewfinder.
 */
private class HeldPreviewSurface(private val executor: Executor) : AutoCloseable {
    private var texture: SurfaceTexture? = null
    private var surface: Surface? = null

    val provider = Preview.SurfaceProvider { request ->
        close()
        val tex = SurfaceTexture(0)
        val width = request.resolution.width.coerceAtLeast(1)
        val height = request.resolution.height.coerceAtLeast(1)
        tex.setDefaultBufferSize(width, height)
        val surf = Surface(tex)
        texture = tex
        surface = surf
        AirControleLog.i("dummy preview surface ${width}x$height")
        request.provideSurface(surf, executor) { }
    }

    override fun close() {
        try {
            surface?.release()
        } catch (error: Throwable) {
            AirControleLog.w("dummy preview surface release failed: ${error.message}", error)
        }
        try {
            texture?.release()
        } catch (error: Throwable) {
            AirControleLog.w("dummy preview texture release failed: ${error.message}", error)
        }
        surface = null
        texture = null
    }
}
