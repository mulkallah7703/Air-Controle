package com.mulkallah.aircontrole.core.model

/**
 * Stable status tokens written to [com.mulkallah.aircontrole.core.bridge.AirControlBridge].
 * Home maps error tokens to actionable Arabic / English copy.
 */
object PipelineStatus {
    const val RUNNING = "running"
    const val STOPPED = "stopped"
    const val CAMERA_BOUND = "camera_bound"
    const val LANDMARKER_READY = "landmarker_ready"
    const val ACCESSIBILITY_CONNECTED = "accessibility_connected"

    const val MODEL_MISSING = "model_missing"
    const val LANDMARKER_FAILED = "landmarker_failed"
    const val CAMERA_START_FAILED = "camera_start_failed"
    const val CAMERA_ERROR = "camera_error"
    const val CAMERA_NO_FRAMES = "camera_no_frames"
    const val CAMERA_PERMISSION = "camera_permission"
    const val OVERLAY_FAILED = "overlay_failed"
    const val SERVICE_START_FAILED = "service_start_failed"
    const val SERVICE_NOT_RUNNING = "service_not_running"

    val errors: Set<String> = setOf(
        MODEL_MISSING,
        LANDMARKER_FAILED,
        CAMERA_START_FAILED,
        CAMERA_ERROR,
        CAMERA_NO_FRAMES,
        CAMERA_PERMISSION,
        OVERLAY_FAILED,
        SERVICE_START_FAILED,
        SERVICE_NOT_RUNNING,
    )

    fun isError(status: String): Boolean = status in errors
}
