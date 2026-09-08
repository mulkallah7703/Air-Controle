package com.mulkallah.aircontrole.ui.gestures

import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.core.model.GestureAction
import com.mulkallah.aircontrole.core.model.GestureMappingCatalog

fun gestureTitleRes(key: String): Int = when (key) {
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

fun gestureHowRes(key: String): Int = when (key) {
    GestureMappingCatalog.CLICK -> R.string.training_how_click
    GestureMappingCatalog.SCROLL_UP -> R.string.training_how_scroll_up
    GestureMappingCatalog.SCROLL_DOWN -> R.string.training_how_scroll_down
    GestureMappingCatalog.SWIPE_LEFT -> R.string.training_how_swipe_left
    GestureMappingCatalog.SWIPE_RIGHT -> R.string.training_how_swipe_right
    GestureMappingCatalog.PALM_PAUSE -> R.string.training_how_palm
    GestureMappingCatalog.FIST_BACK -> R.string.training_how_fist
    GestureMappingCatalog.PEACE_HOME -> R.string.training_how_peace
    GestureMappingCatalog.POINT_MOVE -> R.string.training_how_point
    else -> R.string.training_how_point
}

fun gestureShortRes(key: String): Int = when (key) {
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

fun actionLabelRes(action: GestureAction): Int = when (action) {
    GestureAction.CLICK -> R.string.action_click
    GestureAction.SCROLL_UP -> R.string.action_scroll_up
    GestureAction.SCROLL_DOWN -> R.string.action_scroll_down
    GestureAction.SWIPE_LEFT -> R.string.action_swipe_left
    GestureAction.SWIPE_RIGHT -> R.string.action_swipe_right
    GestureAction.BACK -> R.string.action_back_system
    GestureAction.HOME -> R.string.action_home_system
    GestureAction.PAUSE -> R.string.action_pause
    GestureAction.RECENTS -> R.string.action_recents
    GestureAction.MOVE_CURSOR -> R.string.action_move_cursor
    GestureAction.NONE -> R.string.action_none
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
