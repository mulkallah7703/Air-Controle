package com.mulkallah.aircontrole.ui.gestures

import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.core.model.GestureMappingCatalog

fun gestureTitleRes(key: String): Int = when (key) {
    GestureMappingCatalog.HAND_START -> R.string.gesture_hand_start_title
    GestureMappingCatalog.CLICK -> R.string.gesture_click_title
    GestureMappingCatalog.SCROLL_UP -> R.string.gesture_scroll_up_title
    GestureMappingCatalog.SCROLL_DOWN -> R.string.gesture_scroll_down_title
    GestureMappingCatalog.SWIPE_LEFT -> R.string.gesture_swipe_left_title
    GestureMappingCatalog.SWIPE_RIGHT -> R.string.gesture_swipe_right_title
    GestureMappingCatalog.PALM_PAUSE -> R.string.gesture_palm_title
    GestureMappingCatalog.FIST_BACK -> R.string.gesture_fist_title
    GestureMappingCatalog.PEACE_HOME -> R.string.gesture_peace_title
    GestureMappingCatalog.POINT_MOVE -> R.string.gesture_point_title
    else -> R.string.gesture_point
}

fun gestureGuideBodyRes(key: String): Int = when (key) {
    GestureMappingCatalog.HAND_START -> R.string.guide_hand_start_body
    GestureMappingCatalog.CLICK -> R.string.guide_click_body
    GestureMappingCatalog.SCROLL_UP -> R.string.guide_scroll_up_body
    GestureMappingCatalog.SCROLL_DOWN -> R.string.guide_scroll_down_body
    GestureMappingCatalog.SWIPE_LEFT -> R.string.guide_swipe_left_body
    GestureMappingCatalog.SWIPE_RIGHT -> R.string.guide_swipe_right_body
    GestureMappingCatalog.PALM_PAUSE -> R.string.guide_palm_body
    GestureMappingCatalog.FIST_BACK -> R.string.guide_fist_body
    GestureMappingCatalog.PEACE_HOME -> R.string.guide_peace_body
    GestureMappingCatalog.POINT_MOVE -> R.string.guide_point_body
    else -> R.string.guide_point_body
}

fun gestureShortRes(key: String): Int = when (key) {
    GestureMappingCatalog.HAND_START -> R.string.gesture_hand_start
    "CLICK" -> R.string.gesture_click
    "SCROLL_UP" -> R.string.gesture_scroll_up
    "SCROLL_DOWN" -> R.string.gesture_scroll_down
    "SWIPE_LEFT" -> R.string.gesture_swipe_left
    "SWIPE_RIGHT" -> R.string.gesture_swipe_right
    "PALM_PAUSE" -> R.string.gesture_palm
    "FIST_BACK" -> R.string.gesture_fist
    "PEACE_HOME" -> R.string.gesture_peace
    "POINT_MOVE" -> R.string.gesture_point
    else -> R.string.gesture_point
}

fun machineStateRes(state: String): Int = when (state) {
    "IDLE" -> R.string.state_idle
    "HAND_DETECTED" -> R.string.state_hand_detected
    "TRACKING" -> R.string.state_tracking
    "GESTURE_RECOGNIZED" -> R.string.state_gesture_recognized
    "ACTION" -> R.string.state_action
    "COOLDOWN" -> R.string.state_cooldown
    else -> R.string.state_idle
}

fun pipelineErrorRes(status: String): Int? = when (status) {
    "model_missing" -> R.string.home_error_model_missing
    "landmarker_failed" -> R.string.home_error_landmarker
    "camera_start_failed" -> R.string.home_error_camera_start
    "camera_error" -> R.string.home_error_camera
    "camera_no_frames" -> R.string.home_error_camera_no_frames
    "camera_permission" -> R.string.home_error_camera_permission
    "overlay_failed" -> R.string.home_error_overlay
    "service_start_failed" -> R.string.home_error_service_start
    "service_not_running" -> R.string.home_error_service_not_running
    else -> null
}

fun gestureEmoji(key: String): String = when (key) {
    GestureMappingCatalog.HAND_START -> "👋"
    GestureMappingCatalog.POINT_MOVE -> "☝️"
    GestureMappingCatalog.CLICK -> "🤏"
    GestureMappingCatalog.SCROLL_UP -> "☝️⬆️"
    GestureMappingCatalog.SCROLL_DOWN -> "☝️⬇️"
    GestureMappingCatalog.SWIPE_RIGHT -> "☝️➡️"
    GestureMappingCatalog.SWIPE_LEFT -> "☝️⬅️"
    GestureMappingCatalog.PALM_PAUSE -> "✋"
    GestureMappingCatalog.FIST_BACK -> "✊"
    GestureMappingCatalog.PEACE_HOME -> "✌️"
    else -> "🖐️"
}
