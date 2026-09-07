package com.mulkallah.aircontrole.gestures

import com.mulkallah.aircontrole.core.model.CursorPosition
import com.mulkallah.aircontrole.core.model.HandFrame

/**
 * IDLE → HAND_DETECTED → TRACKING → GESTURE_RECOGNIZED → ACTION → COOLDOWN
 *
 * Discrete actions (click, swipe, scroll, home, back, pause) are edge-triggered
 * and then enter cooldown so a held pose does not spam Accessibility events.
 * Point+hold continuously updates the overlay cursor while staying in TRACKING.
 */
class GestureStateMachine(
    private val classifier: GestureClassifier = GestureClassifier(),
    private val detectHoldMs: Long = 80L,
    private val poseHoldMs: Long = 280L,
    private val cooldownMs: Long = 450L,
    private val swipeSpeed: Float = 1.15f,
    private val lostHandTimeoutMs: Long = 350L,
) {
    var state: GestureState = GestureState.IDLE
        private set

    private var lastSeenMs: Long = 0L
    private var enteredStateMs: Long = 0L
    private var holdPose: HandPose = HandPose.UNKNOWN
    private var holdStartedMs: Long = 0L
    private var previousPinchOpen: Boolean = true
    private var previousTip: ClassificationTip? = null
    private var lastDispatched: GestureType = GestureType.NONE
    private var pendingDispatch: GestureType = GestureType.NONE
    private var cursor = CursorPosition(0.5f, 0.5f, visible = false)

    fun reset() {
        state = GestureState.IDLE
        lastSeenMs = 0L
        enteredStateMs = 0L
        holdPose = HandPose.UNKNOWN
        holdStartedMs = 0L
        previousPinchOpen = true
        previousTip = null
        lastDispatched = GestureType.NONE
        pendingDispatch = GestureType.NONE
        cursor = CursorPosition(0.5f, 0.5f, visible = false)
    }

    fun onLostHand(nowMs: Long): GestureFrameResult {
        if (lastSeenMs != 0L && nowMs - lastSeenMs > lostHandTimeoutMs) {
            reset()
        }
        return currentResult(shouldDispatch = false, recognized = GestureType.NONE, pulse = false)
    }

    fun onFrame(frame: HandFrame): GestureFrameResult {
        lastSeenMs = frame.timestampMs
        val classification = classifier.classify(frame)
        updateCursor(classification)

        if (state == GestureState.COOLDOWN) {
            if (frame.timestampMs - enteredStateMs >= cooldownMs) {
                enter(GestureState.TRACKING, frame.timestampMs)
                previousPinchOpen = classifier.isPinchOpen(classification.pinchDistance)
            }
            rememberTip(classification, frame.timestampMs)
            return currentResult(false, GestureType.NONE, pulse = false)
        }

        if (state == GestureState.ACTION) {
            val dispatched = pendingDispatch
            pendingDispatch = GestureType.NONE
            enter(GestureState.COOLDOWN, frame.timestampMs)
            rememberTip(classification, frame.timestampMs)
            return currentResult(shouldDispatch = true, recognized = dispatched, pulse = true)
        }

        if (state == GestureState.GESTURE_RECOGNIZED) {
            enter(GestureState.ACTION, frame.timestampMs)
            rememberTip(classification, frame.timestampMs)
            return currentResult(false, pendingDispatch, pulse = true)
        }

        when (state) {
            GestureState.IDLE -> {
                if (classification.pose != HandPose.UNKNOWN) {
                    enter(GestureState.HAND_DETECTED, frame.timestampMs)
                }
            }
            GestureState.HAND_DETECTED -> {
                if (classification.pose == HandPose.UNKNOWN) {
                    enter(GestureState.IDLE, frame.timestampMs)
                } else if (frame.timestampMs - enteredStateMs >= detectHoldMs) {
                    enter(GestureState.TRACKING, frame.timestampMs)
                    holdPose = classification.pose
                    holdStartedMs = frame.timestampMs
                }
            }
            GestureState.TRACKING -> {
                val motion = recognizeMotion(classification, frame.timestampMs)
                val discrete = recognizeDiscrete(classification, frame.timestampMs)
                val recognized = when {
                    discrete != GestureType.NONE -> discrete
                    motion != GestureType.NONE -> motion
                    else -> GestureType.NONE
                }
                if (recognized != GestureType.NONE) {
                    pendingDispatch = recognized
                    lastDispatched = recognized
                    enter(GestureState.GESTURE_RECOGNIZED, frame.timestampMs)
                    rememberTip(classification, frame.timestampMs)
                    return currentResult(false, recognized, pulse = true)
                }
            }
            else -> Unit
        }

        rememberTip(classification, frame.timestampMs)
        return currentResult(false, GestureType.NONE, pulse = false)
    }

    private fun recognizeDiscrete(classification: GestureClassifier.Classification, nowMs: Long): GestureType {
        val pinchClosed = classifier.isPinchClosed(classification.pinchDistance)
        val pinchOpen = classifier.isPinchOpen(classification.pinchDistance)
        if (previousPinchOpen && pinchClosed) {
            previousPinchOpen = false
            return GestureType.CLICK
        }
        if (pinchOpen) {
            previousPinchOpen = true
        }

        if (classification.pose != holdPose) {
            holdPose = classification.pose
            holdStartedMs = nowMs
            return GestureType.NONE
        }
        if (nowMs - holdStartedMs < poseHoldMs) {
            return GestureType.NONE
        }

        return when (classification.pose) {
            HandPose.OPEN_PALM -> {
                holdStartedMs = nowMs + cooldownMs
                GestureType.PALM_PAUSE
            }
            HandPose.FIST -> {
                holdStartedMs = nowMs + cooldownMs
                GestureType.FIST_BACK
            }
            HandPose.PEACE -> {
                holdStartedMs = nowMs + cooldownMs
                GestureType.PEACE_HOME
            }
            else -> GestureType.NONE
        }
    }

    private fun recognizeMotion(classification: GestureClassifier.Classification, nowMs: Long): GestureType {
        val previous = previousTip ?: return GestureType.NONE
        if (classification.pose != HandPose.POINT && classification.pose != HandPose.UNKNOWN) {
            return GestureType.NONE
        }
        if (classification.pose != HandPose.POINT) return GestureType.NONE
        val (vx, vy) = velocity(previous.point, classification.fingertip, nowMs - previous.timestampMs)
        return dominantSwipe(vx, vy, swipeSpeed)
    }

    private fun updateCursor(classification: GestureClassifier.Classification) {
        val visible = state != GestureState.IDLE && classification.pose != HandPose.UNKNOWN
        // Front camera is mirrored so a rightward finger move matches screen space.
        val mirroredX = 1f - classification.fingertip.x
        cursor = CursorPosition(
            x = mirroredX.coerceIn(0f, 1f),
            y = classification.fingertip.y.coerceIn(0f, 1f),
            visible = visible,
        )
    }

    private fun rememberTip(classification: GestureClassifier.Classification, nowMs: Long) {
        previousTip = ClassificationTip(classification.fingertip, nowMs)
    }

    private fun enter(next: GestureState, nowMs: Long) {
        state = next
        enteredStateMs = nowMs
    }

    private fun currentResult(
        shouldDispatch: Boolean,
        recognized: GestureType,
        pulse: Boolean,
    ): GestureFrameResult {
        val pose = holdPose
        return GestureFrameResult(
            state = state,
            pose = pose,
            recognized = recognized,
            cursor = cursor,
            pulse = pulse,
            shouldDispatch = shouldDispatch,
        )
    }

    private data class ClassificationTip(
        val point: com.mulkallah.aircontrole.core.model.NormalizedPoint,
        val timestampMs: Long,
    )
}
