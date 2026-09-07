package com.mulkallah.aircontrole.gestures

import com.mulkallah.aircontrole.core.model.HandFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureStateMachineTest {

    @Test
    fun walksIdleToTrackingWhenHandStaysVisible() {
        val machine = GestureStateMachine(detectHoldMs = 50L)
        val frame = HandFrame(baseLandmarks(index = true, middle = false, ring = false, pinky = false), 0L)
        assertEquals(GestureState.IDLE, machine.state)
        machine.onFrame(frame)
        assertEquals(GestureState.HAND_DETECTED, machine.state)
        val later = frame.copy(timestampMs = 80L)
        machine.onFrame(later)
        assertEquals(GestureState.TRACKING, machine.state)
    }

    @Test
    fun pinchEdgeDispatchesClickAndEntersCooldown() {
        val machine = GestureStateMachine(detectHoldMs = 10L, cooldownMs = 200L)
        var t = 0L
        fun pointFrame(time: Long) = HandFrame(
            baseLandmarks(index = true, middle = false, ring = false, pinky = false),
            time,
        )
        machine.onFrame(pointFrame(t))
        t += 20
        machine.onFrame(pointFrame(t))
        assertEquals(GestureState.TRACKING, machine.state)

        val pinchLandmarks = baseLandmarks(index = true, middle = false, ring = false, pinky = false).toMutableList()
        pinchLandmarks[HandFrame.THUMB_TIP] = pinchLandmarks[HandFrame.INDEX_TIP]
        val pinch = HandFrame(pinchLandmarks, t + 20)
        val recognized = machine.onFrame(pinch)
        assertEquals(GestureType.CLICK, recognized.recognized)
        assertEquals(GestureState.GESTURE_RECOGNIZED, machine.state)

        val action = machine.onFrame(pinch.copy(timestampMs = pinch.timestampMs + 10))
        assertEquals(GestureState.ACTION, action.state)

        val dispatched = machine.onFrame(pinch.copy(timestampMs = pinch.timestampMs + 20))
        assertTrue(dispatched.shouldDispatch)
        assertEquals(GestureType.CLICK, dispatched.recognized)
        assertEquals(GestureState.COOLDOWN, dispatched.state)
    }

    @Test
    fun lostHandReturnsToIdleAfterTimeout() {
        val machine = GestureStateMachine(detectHoldMs = 10L, lostHandTimeoutMs = 100L)
        val frame = HandFrame(baseLandmarks(true, false, false, false), 0L)
        machine.onFrame(frame)
        machine.onFrame(frame.copy(timestampMs = 20L))
        assertEquals(GestureState.TRACKING, machine.state)
        machine.onLostHand(200L)
        assertEquals(GestureState.IDLE, machine.state)
    }
}
