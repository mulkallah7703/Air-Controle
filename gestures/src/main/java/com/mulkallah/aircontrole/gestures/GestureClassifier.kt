package com.mulkallah.aircontrole.gestures

import com.mulkallah.aircontrole.core.model.HandFrame
import com.mulkallah.aircontrole.core.model.NormalizedPoint
import kotlin.math.abs

class GestureClassifier(
    private val pinchClose: Float = 0.055f,
    private val pinchOpen: Float = 0.09f,
    private val extensionRatio: Float = 1.18f,
) {
    fun classify(frame: HandFrame): Classification {
        if (!frame.isValid) {
            return Classification(HandPose.UNKNOWN, 0f)
        }
        val landmarks = frame.landmarks
        val index = isExtended(landmarks, HandFrame.INDEX_TIP, HandFrame.INDEX_PIP, HandFrame.INDEX_MCP)
        val middle = isExtended(landmarks, HandFrame.MIDDLE_TIP, HandFrame.MIDDLE_PIP, HandFrame.MIDDLE_MCP)
        val ring = isExtended(landmarks, HandFrame.RING_TIP, HandFrame.RING_PIP, HandFrame.RING_MCP)
        val pinky = isExtended(landmarks, HandFrame.PINKY_TIP, HandFrame.PINKY_PIP, HandFrame.PINKY_MCP)
        val pinchDistance = landmarks[HandFrame.THUMB_TIP].distanceTo(landmarks[HandFrame.INDEX_TIP])

        val pose = when {
            pinchDistance < pinchClose -> HandPose.PINCH
            index && middle && !ring && !pinky -> HandPose.PEACE
            index && !middle && !ring && !pinky -> HandPose.POINT
            index && middle && ring && pinky -> HandPose.OPEN_PALM
            !index && !middle && !ring && !pinky -> HandPose.FIST
            else -> HandPose.UNKNOWN
        }

        return Classification(
            pose = pose,
            pinchDistance = pinchDistance,
            fingertip = landmarks[HandFrame.INDEX_TIP],
        )
    }

    fun isPinchClosed(distance: Float): Boolean = distance < pinchClose

    fun isPinchOpen(distance: Float): Boolean = distance > pinchOpen

    private fun isExtended(
        landmarks: List<NormalizedPoint>,
        tip: Int,
        pip: Int,
        mcp: Int,
    ): Boolean {
        val wrist = landmarks[HandFrame.WRIST]
        val tipDist = wrist.distanceTo(landmarks[tip])
        val pipDist = wrist.distanceTo(landmarks[pip])
        val mcpDist = wrist.distanceTo(landmarks[mcp])
        val baseline = maxOf(pipDist, mcpDist)
        return tipDist > baseline * extensionRatio
    }

    data class Classification(
        val pose: HandPose,
        val pinchDistance: Float,
        val fingertip: NormalizedPoint = NormalizedPoint(0.5f, 0.5f),
    )
}

fun velocity(from: NormalizedPoint, to: NormalizedPoint, dtMs: Long): Pair<Float, Float> {
    if (dtMs <= 0L) return 0f to 0f
    val seconds = dtMs / 1000f
    return ((to.x - from.x) / seconds) to ((to.y - from.y) / seconds)
}

fun dominantSwipe(vx: Float, vy: Float, threshold: Float): GestureType {
    return when {
        abs(vx) > threshold && abs(vx) > abs(vy) * 1.35f && vx > 0f -> GestureType.SWIPE_RIGHT
        abs(vx) > threshold && abs(vx) > abs(vy) * 1.35f && vx < 0f -> GestureType.SWIPE_LEFT
        abs(vy) > threshold && abs(vy) > abs(vx) * 1.35f && vy < 0f -> GestureType.SCROLL_UP
        abs(vy) > threshold && abs(vy) > abs(vx) * 1.35f && vy > 0f -> GestureType.SCROLL_DOWN
        else -> GestureType.NONE
    }
}
