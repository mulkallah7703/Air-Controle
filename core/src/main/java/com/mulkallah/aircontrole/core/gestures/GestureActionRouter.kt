package com.mulkallah.aircontrole.core.gestures

import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.bridge.AirControlBridge.ScrollDirection
import com.mulkallah.aircontrole.core.bridge.AirControlBridge.SwipeDirection
import com.mulkallah.aircontrole.core.model.CursorPosition
import com.mulkallah.aircontrole.core.model.GestureAction

/**
 * Maps a fixed [GestureAction] to the Accessibility sink (or in-app pause).
 */
object GestureActionRouter {
    fun dispatch(
        action: GestureAction,
        sink: AirControlBridge.ActionSink?,
        cursor: CursorPosition,
        onPause: () -> Unit,
    ): Boolean {
        return when (action) {
            GestureAction.CLICK -> sink?.performClick(cursor.x, cursor.y) ?: false
            GestureAction.SCROLL_UP -> sink?.performScroll(ScrollDirection.UP) ?: false
            GestureAction.SCROLL_DOWN -> sink?.performScroll(ScrollDirection.DOWN) ?: false
            GestureAction.SWIPE_LEFT -> sink?.performSwipe(SwipeDirection.LEFT) ?: false
            GestureAction.SWIPE_RIGHT -> sink?.performSwipe(SwipeDirection.RIGHT) ?: false
            GestureAction.BACK -> sink?.performBack() ?: false
            GestureAction.HOME -> sink?.performHome() ?: false
            GestureAction.PAUSE -> {
                onPause()
                true
            }
            GestureAction.RECENTS -> sink?.performRecents() ?: false
            GestureAction.MOVE_CURSOR, GestureAction.NONE -> false
        }
    }
}
