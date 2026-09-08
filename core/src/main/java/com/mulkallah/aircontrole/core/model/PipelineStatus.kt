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

    /**
     * Map a camera / MediaPipe failure to a Home token. Walks the cause chain so
     * CameraX "surface combination" and native-load errors are not dumped as a
     * generic [CAMERA_ERROR].
     */
    fun fromThrowable(error: Throwable): String {
        val chain = generateSequence(error) { it.cause }.toList()
        val messages = chain.map { it.message.orEmpty() }
        val classes = chain.map { it.javaClass.simpleName }
        val blob = (messages + classes).joinToString(" ").lowercase()
        return when {
            messages.any { it == MODEL_MISSING } || blob.contains("hand_landmarker") && blob.contains("missing") ->
                MODEL_MISSING
            blob.contains("hand_landmarker.task") && (blob.contains("asset") || blob.contains("open") || blob.contains("found")) ->
                MODEL_MISSING
            messages.any { it == CAMERA_PERMISSION } || blob.contains("permission") && blob.contains("camera") ->
                CAMERA_PERMISSION
            messages.any { it == LANDMARKER_FAILED } ||
                blob.contains("landmarker") ||
                blob.contains("mediapipe") ||
                blob.contains("tflite") ||
                blob.contains("unsatisfiedlink") ||
                blob.contains("dlopen") ->
                LANDMARKER_FAILED
            messages.any { it == CAMERA_START_FAILED } ||
                blob.contains("surface combination") ||
                blob.contains("cameraunavailable") ||
                blob.contains("cameraaccessexception") ||
                blob.contains("bind") ||
                blob.contains("camera") ->
                CAMERA_START_FAILED
            else -> CAMERA_ERROR
        }
    }
}
