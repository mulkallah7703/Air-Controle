package com.mulkallah.aircontrole.core.model

/**
 * Fixed gesture → action table for every user. Keys match
 * [com.mulkallah.aircontrole.gestures.GestureType] names.
 */
object GestureMappingCatalog {
    const val CLICK = "CLICK"
    const val SCROLL_UP = "SCROLL_UP"
    const val SCROLL_DOWN = "SCROLL_DOWN"
    const val SWIPE_LEFT = "SWIPE_LEFT"
    const val SWIPE_RIGHT = "SWIPE_RIGHT"
    const val PALM_PAUSE = "PALM_PAUSE"
    const val FIST_BACK = "FIST_BACK"
    const val PEACE_HOME = "PEACE_HOME"
    const val POINT_MOVE = "POINT_MOVE"
    const val HAND_START = "HAND_START"

    val gestureKeys: List<String> = listOf(
        CLICK,
        SCROLL_UP,
        SCROLL_DOWN,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        PALM_PAUSE,
        FIST_BACK,
        PEACE_HOME,
        POINT_MOVE,
    )

    /** Guide order: start first, then motion, then held poses. */
    val guideKeys: List<String> = listOf(
        HAND_START,
        POINT_MOVE,
        CLICK,
        SCROLL_UP,
        SCROLL_DOWN,
        SWIPE_RIGHT,
        SWIPE_LEFT,
        PALM_PAUSE,
        FIST_BACK,
        PEACE_HOME,
    )

    val defaults: Map<String, GestureAction> = mapOf(
        CLICK to GestureAction.CLICK,
        SCROLL_UP to GestureAction.SCROLL_UP,
        SCROLL_DOWN to GestureAction.SCROLL_DOWN,
        SWIPE_LEFT to GestureAction.SWIPE_LEFT,
        SWIPE_RIGHT to GestureAction.SWIPE_RIGHT,
        PALM_PAUSE to GestureAction.PAUSE,
        FIST_BACK to GestureAction.BACK,
        PEACE_HOME to GestureAction.HOME,
        POINT_MOVE to GestureAction.MOVE_CURSOR,
    )

    fun actionFor(gestureKey: String): GestureAction {
        return defaults[gestureKey] ?: GestureAction.NONE
    }
}
