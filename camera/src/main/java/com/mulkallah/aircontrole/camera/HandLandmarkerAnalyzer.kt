package com.mulkallah.aircontrole.camera

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.mulkallah.aircontrole.core.model.HandFrame
import com.mulkallah.aircontrole.core.model.NormalizedPoint

class HandLandmarkerAnalyzer(
    context: Context,
    private val onHand: (HandFrame) -> Unit,
    private val onEmpty: (Long) -> Unit,
    private val onError: (Throwable) -> Unit,
) : ImageAnalysis.Analyzer {

    private val landmarker: HandLandmarker? = try {
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .build(),
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.55f)
            .setMinHandPresenceConfidence(0.55f)
            .setMinTrackingConfidence(0.55f)
            .setResultListener(::onResult)
            .setErrorListener { error -> onError(error) }
            .build()
        HandLandmarker.createFromOptions(context, options)
    } catch (error: Throwable) {
        onError(error)
        null
    }

    override fun analyze(imageProxy: ImageProxy) {
        val marker = landmarker
        if (marker == null) {
            imageProxy.close()
            return
        }
        try {
            val mpImage = BitmapImageBuilder(imageProxy.toBitmap()).build()
            val timestamp = imageProxy.imageInfo.timestamp.takeIf { it > 0L }
                ?: SystemClock.uptimeMillis()
            marker.detectAsync(mpImage, timestamp)
        } catch (error: Throwable) {
            onError(error)
        } finally {
            imageProxy.close()
        }
    }

    private fun onResult(result: HandLandmarkerResult, @Suppress("UNUSED_PARAMETER") image: MPImage) {
        val now = SystemClock.uptimeMillis()
        val landmarks = result.landmarks().firstOrNull()
        if (landmarks.isNullOrEmpty()) {
            onEmpty(now)
            return
        }
        val points = landmarks.map { landmark ->
            NormalizedPoint(landmark.x(), landmark.y(), landmark.z())
        }
        val handedness = result.handedness().firstOrNull()?.firstOrNull()?.categoryName()
        onHand(HandFrame(points, now, handedness))
    }

    fun close() {
        landmarker?.close()
    }

    companion object {
        const val MODEL_ASSET = "hand_landmarker.task"
    }
}
