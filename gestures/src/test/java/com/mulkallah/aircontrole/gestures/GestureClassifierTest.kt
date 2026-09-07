package com.mulkallah.aircontrole.gestures

import com.mulkallah.aircontrole.core.model.HandFrame
import com.mulkallah.aircontrole.core.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureClassifierTest {
    private val classifier = GestureClassifier()

    @Test
    fun openPalmWhenFourFingersExtended() {
        val frame = hand(index = true, middle = true, ring = true, pinky = true)
        assertEquals(HandPose.OPEN_PALM, classifier.classify(frame).pose)
    }

    @Test
    fun fistWhenNoFingersExtended() {
        val frame = hand(index = false, middle = false, ring = false, pinky = false)
        assertEquals(HandPose.FIST, classifier.classify(frame).pose)
    }

    @Test
    fun peaceWhenIndexAndMiddleExtended() {
        val frame = hand(index = true, middle = true, ring = false, pinky = false)
        assertEquals(HandPose.PEACE, classifier.classify(frame).pose)
    }

    @Test
    fun pointWhenOnlyIndexExtended() {
        val frame = hand(index = true, middle = false, ring = false, pinky = false)
        assertEquals(HandPose.POINT, classifier.classify(frame).pose)
    }

    @Test
    fun pinchWhenThumbMeetsIndex() {
        val landmarks = baseLandmarks(index = true, middle = false, ring = false, pinky = false).toMutableList()
        landmarks[HandFrame.THUMB_TIP] = landmarks[HandFrame.INDEX_TIP].copy(
            x = landmarks[HandFrame.INDEX_TIP].x + 0.01f,
            y = landmarks[HandFrame.INDEX_TIP].y + 0.01f,
        )
        val frame = HandFrame(landmarks, timestampMs = 0L)
        assertEquals(HandPose.PINCH, classifier.classify(frame).pose)
    }

    private fun hand(
        index: Boolean,
        middle: Boolean,
        ring: Boolean,
        pinky: Boolean,
    ): HandFrame = HandFrame(baseLandmarks(index, middle, ring, pinky), timestampMs = 1L)
}

internal fun baseLandmarks(
    index: Boolean,
    middle: Boolean,
    ring: Boolean,
    pinky: Boolean,
): List<NormalizedPoint> {
    val points = MutableList(21) { NormalizedPoint(0.5f, 0.6f) }
    points[HandFrame.WRIST] = NormalizedPoint(0.5f, 0.8f)

    fun placeFinger(mcp: Int, pip: Int, dip: Int, tip: Int, x: Float, extended: Boolean) {
        points[mcp] = NormalizedPoint(x, 0.62f)
        points[pip] = NormalizedPoint(x, if (extended) 0.50f else 0.64f)
        points[dip] = NormalizedPoint(x, if (extended) 0.40f else 0.65f)
        points[tip] = NormalizedPoint(x, if (extended) 0.22f else 0.66f)
    }

    placeFinger(HandFrame.INDEX_MCP, HandFrame.INDEX_PIP, HandFrame.INDEX_DIP, HandFrame.INDEX_TIP, 0.42f, index)
    placeFinger(HandFrame.MIDDLE_MCP, HandFrame.MIDDLE_PIP, HandFrame.MIDDLE_DIP, HandFrame.MIDDLE_TIP, 0.50f, middle)
    placeFinger(HandFrame.RING_MCP, HandFrame.RING_PIP, HandFrame.RING_DIP, HandFrame.RING_TIP, 0.58f, ring)
    placeFinger(HandFrame.PINKY_MCP, HandFrame.PINKY_PIP, HandFrame.PINKY_DIP, HandFrame.PINKY_TIP, 0.66f, pinky)

    points[HandFrame.THUMB_CMC] = NormalizedPoint(0.36f, 0.72f)
    points[HandFrame.THUMB_MCP] = NormalizedPoint(0.32f, 0.68f)
    points[HandFrame.THUMB_IP] = NormalizedPoint(0.28f, 0.64f)
    points[HandFrame.THUMB_TIP] = NormalizedPoint(0.24f, 0.58f)
    return points
}
