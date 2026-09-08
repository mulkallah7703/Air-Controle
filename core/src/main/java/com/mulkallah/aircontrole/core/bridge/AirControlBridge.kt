package com.mulkallah.aircontrole.core.bridge

import com.mulkallah.aircontrole.core.model.CursorPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide coordination between the foreground service, overlay, and
 * Accessibility Service. The Accessibility Service registers itself when
 * connected so the control loop can request Home / Back / click / scroll.
 */
object AirControlBridge {
    interface ActionSink {
        fun performHome(): Boolean
        fun performBack(): Boolean
        fun performClick(x: Float, y: Float): Boolean
        fun performScroll(direction: ScrollDirection): Boolean
        fun performSwipe(direction: SwipeDirection): Boolean
        fun performRecents(): Boolean
        fun openApplication(packageName: String): Boolean
        val connected: Boolean
    }

    enum class ScrollDirection { UP, DOWN }

    enum class SwipeDirection { LEFT, RIGHT }

    @Volatile
    var actionSink: ActionSink? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    private val _machineState = MutableStateFlow("IDLE")
    val machineState: StateFlow<String> = _machineState.asStateFlow()

    private val _lastGesture = MutableStateFlow<String?>(null)
    val lastGesture: StateFlow<String?> = _lastGesture.asStateFlow()

    private val _recognitionSeq = MutableStateFlow(0L)
    val recognitionSeq: StateFlow<Long> = _recognitionSeq.asStateFlow()

    private val _pose = MutableStateFlow("UNKNOWN")
    val pose: StateFlow<String> = _pose.asStateFlow()

    private val _suppressActions = MutableStateFlow(false)
    val suppressActions: StateFlow<Boolean> = _suppressActions.asStateFlow()

    private val _cursor = MutableStateFlow(CursorPosition(0.5f, 0.5f, visible = false))
    val cursor: StateFlow<CursorPosition> = _cursor.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun setRunning(value: Boolean) {
        _running.value = value
        if (!value) {
            _paused.value = false
            _machineState.value = "IDLE"
            _pose.value = "UNKNOWN"
            _cursor.value = _cursor.value.copy(visible = false)
        }
    }

    fun setPaused(value: Boolean) {
        _paused.value = value
    }

    fun togglePaused() {
        _paused.value = !_paused.value
    }

    fun updateMachineState(state: String) {
        _machineState.value = state
    }

    fun updateLastGesture(gesture: String?) {
        _lastGesture.value = gesture
        if (gesture != null) {
            _recognitionSeq.value += 1
        }
    }

    fun updatePose(pose: String) {
        _pose.value = pose
    }

    fun setSuppressActions(value: Boolean) {
        _suppressActions.value = value
    }

    fun updateCursor(position: CursorPosition) {
        _cursor.value = position
    }

    fun updateStatus(message: String) {
        _statusMessage.value = message
    }

    val accessibilityConnected: Boolean
        get() = actionSink?.connected == true
}
