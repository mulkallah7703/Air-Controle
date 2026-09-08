package com.mulkallah.aircontrole.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.model.HandFrame
import com.mulkallah.aircontrole.core.model.NormalizedPoint
import com.mulkallah.aircontrole.core.model.PipelineStatus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class HandLandmarkerAnalyzer(
    context: Context,
    private val onHand: (HandFrame) -> Unit,
    private val onEmpty: (Long) -> Unit,
    private val onError: (Throwable) -> Unit,
) : ImageAnalysis.Analyzer {

    private val busy = AtomicBoolean(false)
    private val frames = AtomicInteger(0)
    private val lastFpsLogMs = AtomicLong(0L)
    private val lastLandmarkCount = AtomicInteger(0)
    private val timestampMs = AtomicLong(0L)

    private val landmarker: HandLandmarker? = createLandmarker(context)

    val isReady: Boolean get() = landmarker != null

    override fun analyze(imageProxy: ImageProxy) {
        val marker = landmarker
        if (marker == null) {
            imageProxy.close()
            return
        }
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        try {
            val upright = imageProxy.toUprightBitmap()
            val mpImage = BitmapImageBuilder(upright).build()
            val now = nextTimestamp()
            logFrameRate(now)
            marker.detectAsync(mpImage, now)
        } catch (error: Throwable) {
            busy.set(false)
            AirControleLog.e("landmarker analyze failed", error)
            onError(error)
        } finally {
            imageProxy.close()
        }
    }

    private fun onResult(result: HandLandmarkerResult, @Suppress("UNUSED_PARAMETER") image: MPImage) {
        busy.set(false)
        val now = SystemClock.uptimeMillis()
        val landmarks = result.landmarks().firstOrNull()
        val count = landmarks?.size ?: 0
        lastLandmarkCount.set(count)
        if (landmarks.isNullOrEmpty()) {
            onEmpty(now)
            return
        }
        val points = landmarks.map { landmark ->
            NormalizedPoint(landmark.x(), landmark.y(), landmark.z())
        }
        val handedness = result.handedness().firstOrNull()?.firstOrNull()?.categoryName()
        AirControleLog.d("landmarks=$count handedness=$handedness")
        onHand(HandFrame(points, now, handedness))
    }

    private fun nextTimestamp(): Long {
        val now = SystemClock.uptimeMillis()
        return timestampMs.updateAndGet { previous ->
            if (now > previous) now else previous + 1L
        }
    }

    private fun logFrameRate(now: Long) {
        val count = frames.incrementAndGet()
        val last = lastFpsLogMs.get()
        if (last == 0L) {
            lastFpsLogMs.set(now)
            return
        }
        val elapsed = now - last
        if (elapsed >= 2_000L && lastFpsLogMs.compareAndSet(last, now)) {
            val fps = (count * 1000f / elapsed).toInt()
            frames.set(0)
            AirControleLog.i("frame rate ~${fps}/s lastLandmarks=${lastLandmarkCount.get()}")
        }
    }

    fun close() {
        busy.set(false)
        try {
            landmarker?.close()
        } catch (error: Throwable) {
            AirControleLog.w("landmarker close failed", error)
        }
    }

    private fun createLandmarker(context: Context): HandLandmarker? {
        if (!modelAssetPresent(context)) {
            val error = IllegalStateException(PipelineStatus.MODEL_MISSING)
            AirControleLog.e("MediaPipe model missing: $MODEL_ASSET")
            onError(error)
            return null
        }
        return try {
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(MODEL_ASSET)
                        .setDelegate(Delegate.CPU)
                        .build(),
                )
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.40f)
                .setMinHandPresenceConfidence(0.40f)
                .setMinTrackingConfidence(0.40f)
                .setResultListener(::onResult)
                .setErrorListener { error ->
                    AirControleLog.e("landmarker runtime error", error)
                    onError(error)
                }
                .build()
            val created = HandLandmarker.createFromOptions(context, options)
            AirControleLog.i("landmarker ready model=$MODEL_ASSET delegate=CPU")
            created
        } catch (error: Throwable) {
            AirControleLog.e("landmarker create failed", error)
            onError(error)
            null
        }
    }

    companion object {
        const val MODEL_ASSET = "hand_landmarker.task"

        fun modelAssetPresent(context: Context): Boolean {
            return try {
                context.assets.open(MODEL_ASSET).use { stream -> stream.available() > 0 }
            } catch (_: Exception) {
                false
            }
        }
    }
}

private fun ImageProxy.toUprightBitmap(): Bitmap {
    val source = toBitmap()
    val degrees = imageInfo.rotationDegrees
    if (degrees == 0) return source
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}
