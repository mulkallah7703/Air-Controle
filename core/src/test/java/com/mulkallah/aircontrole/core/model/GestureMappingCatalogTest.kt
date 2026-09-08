package com.mulkallah.aircontrole.core.model

import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.gestures.GestureActionRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMappingCatalogTest {

    @Test
    fun defaultsAreUnifiedForEveryone() {
        assertEquals(GestureAction.CLICK, GestureMappingCatalog.actionFor(GestureMappingCatalog.CLICK))
        assertEquals(GestureAction.SCROLL_UP, GestureMappingCatalog.actionFor(GestureMappingCatalog.SCROLL_UP))
        assertEquals(GestureAction.SCROLL_DOWN, GestureMappingCatalog.actionFor(GestureMappingCatalog.SCROLL_DOWN))
        assertEquals(GestureAction.SWIPE_LEFT, GestureMappingCatalog.actionFor(GestureMappingCatalog.SWIPE_LEFT))
        assertEquals(GestureAction.SWIPE_RIGHT, GestureMappingCatalog.actionFor(GestureMappingCatalog.SWIPE_RIGHT))
        assertEquals(GestureAction.PAUSE, GestureMappingCatalog.actionFor(GestureMappingCatalog.PALM_PAUSE))
        assertEquals(GestureAction.BACK, GestureMappingCatalog.actionFor(GestureMappingCatalog.FIST_BACK))
        assertEquals(GestureAction.HOME, GestureMappingCatalog.actionFor(GestureMappingCatalog.PEACE_HOME))
        assertEquals(GestureAction.MOVE_CURSOR, GestureMappingCatalog.actionFor(GestureMappingCatalog.POINT_MOVE))
        assertEquals(GestureAction.NONE, GestureMappingCatalog.actionFor("unknown"))
    }

    @Test
    fun noneDisablesDispatch() {
        val sink = RecordingSink()
        var paused = false
        val ran = GestureActionRouter.dispatch(
            action = GestureAction.NONE,
            sink = sink,
            cursor = CursorPosition(0.4f, 0.6f),
            onPause = { paused = true },
        )
        assertFalse(ran)
        assertFalse(paused)
        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun swipeRightRoutesToNextAndSwipeLeftToPrevious() {
        val sink = RecordingSink()
        assertTrue(
            GestureActionRouter.dispatch(
                action = GestureMappingCatalog.actionFor(GestureMappingCatalog.SWIPE_RIGHT),
                sink = sink,
                cursor = CursorPosition(0.5f, 0.5f),
                onPause = {},
            ),
        )
        assertTrue(
            GestureActionRouter.dispatch(
                action = GestureMappingCatalog.actionFor(GestureMappingCatalog.SWIPE_LEFT),
                sink = sink,
                cursor = CursorPosition(0.5f, 0.5f),
                onPause = {},
            ),
        )
        assertEquals(listOf("swipe:RIGHT", "swipe:LEFT"), sink.calls)
    }

    @Test
    fun guideIncludesStartThenCoreGestures() {
        assertEquals(GestureMappingCatalog.HAND_START, GestureMappingCatalog.guideKeys.first())
        assertTrue(GestureMappingCatalog.guideKeys.containsAll(GestureMappingCatalog.gestureKeys))
    }

    private class RecordingSink : AirControlBridge.ActionSink {
        val calls = mutableListOf<String>()
        override val connected: Boolean = true
        override fun performHome(): Boolean {
            calls += "home"
            return true
        }
        override fun performBack(): Boolean {
            calls += "back"
            return true
        }
        override fun performClick(x: Float, y: Float): Boolean {
            calls += "click"
            return true
        }
        override fun performScroll(direction: AirControlBridge.ScrollDirection): Boolean {
            calls += "scroll:${direction.name}"
            return true
        }
        override fun performSwipe(direction: AirControlBridge.SwipeDirection): Boolean {
            calls += "swipe:${direction.name}"
            return true
        }
        override fun performRecents(): Boolean {
            calls += "recents"
            return true
        }
        override fun openApplication(packageName: String): Boolean = false
    }
}
