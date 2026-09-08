package com.mulkallah.aircontrole.core.model

/**
 * Actions the Accessibility Service (or in-app pause) can perform.
 * [MOVE_CURSOR] is overlay-only and does not inject input.
 */
enum class GestureAction {
    CLICK,
    SCROLL_UP,
    SCROLL_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    BACK,
    HOME,
    PAUSE,
    RECENTS,
    MOVE_CURSOR,
    NONE,
    ;

    companion object {
        val assignable: List<GestureAction> = listOf(
            CLICK,
            SCROLL_UP,
            SCROLL_DOWN,
            SWIPE_LEFT,
            SWIPE_RIGHT,
            BACK,
            HOME,
            PAUSE,
            RECENTS,
            MOVE_CURSOR,
            NONE,
        )

        fun fromStorage(value: String?): GestureAction {
            if (value.isNullOrBlank()) return NONE
            return entries.find { it.name == value } ?: NONE
        }
    }
}
