package com.mulkallah.aircontrole.core.model

/**
 * Default gesture → action table. Keys match [com.mulkallah.aircontrole.gestures.GestureType]
 * names so the control loop can resolve mappings at runtime without hardcoding actions.
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

    val defaults: Map<String, GestureAction> = mapOf(
        CLICK to GestureAction.CLICK,
        SCROLL_UP to GestureAction.SCROLL_UP,
        SCROLL_DOWN to GestureAction.SCROLL_DOWN,
        SWIPE_LEFT to GestureAction.BACK,
        SWIPE_RIGHT to GestureAction.HOME,
        PALM_PAUSE to GestureAction.PAUSE,
        FIST_BACK to GestureAction.BACK,
        PEACE_HOME to GestureAction.HOME,
        POINT_MOVE to GestureAction.MOVE_CURSOR,
    )

    fun resolve(stored: Map<String, String>): Map<String, GestureAction> {
        return gestureKeys.associateWith { key ->
            val raw = stored[key]
            if (raw.isNullOrBlank()) {
                defaults.getValue(key)
            } else {
                GestureAction.fromStorage(raw)
            }
        }
    }

    fun actionFor(gestureKey: String, mappings: Map<String, GestureAction>): GestureAction {
        return mappings[gestureKey] ?: defaults[gestureKey] ?: GestureAction.NONE
    }
}
