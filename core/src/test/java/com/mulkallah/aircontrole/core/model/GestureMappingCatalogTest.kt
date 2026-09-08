package com.mulkallah.aircontrole.core.model

import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.gestures.GestureActionRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMappingCatalogTest {

    @Test
    fun defaultsMatchShippedBehavior() {
        val resolved = GestureMappingCatalog.resolve(emptyMap())
        assertEquals(GestureAction.CLICK, resolved[GestureMappingCatalog.CLICK])
        assertEquals(GestureAction.SCROLL_UP, resolved[GestureMappingCatalog.SCROLL_UP])
        assertEquals(GestureAction.SCROLL_DOWN, resolved[GestureMappingCatalog.SCROLL_DOWN])
        assertEquals(GestureAction.BACK, resolved[GestureMappingCatalog.SWIPE_LEFT])
        assertEquals(GestureAction.HOME, resolved[GestureMappingCatalog.SWIPE_RIGHT])
        assertEquals(GestureAction.PAUSE, resolved[GestureMappingCatalog.PALM_PAUSE])
        assertEquals(GestureAction.BACK, resolved[GestureMappingCatalog.FIST_BACK])
        assertEquals(GestureAction.HOME, resolved[GestureMappingCatalog.PEACE_HOME])
        assertEquals(GestureAction.MOVE_CURSOR, resolved[GestureMappingCatalog.POINT_MOVE])
    }

    @Test
    fun storedOverrideWinsAndUnknownFallsBackToNone() {
        val resolved = GestureMappingCatalog.resolve(
            mapOf(
                GestureMappingCatalog.FIST_BACK to GestureAction.RECENTS.name,
                GestureMappingCatalog.CLICK to "not_a_real_action",
            ),
        )
        assertEquals(GestureAction.RECENTS, resolved[GestureMappingCatalog.FIST_BACK])
        assertEquals(GestureAction.NONE, resolved[GestureMappingCatalog.CLICK])
        assertEquals(GestureAction.PAUSE, resolved[GestureMappingCatalog.PALM_PAUSE])
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
    fun customMappingRoutesToSink() {
        val sink = RecordingSink()
        val mappings = GestureMappingCatalog.resolve(
            mapOf(GestureMappingCatalog.PEACE_HOME to GestureAction.RECENTS.name),
        )
        val action = GestureMappingCatalog.actionFor(GestureMappingCatalog.PEACE_HOME, mappings)
        assertEquals(GestureAction.RECENTS, action)
        assertTrue(
            GestureActionRouter.dispatch(
                action = action,
                sink = sink,
                cursor = CursorPosition(0.5f, 0.5f),
                onPause = {},
            ),
        )
        assertEquals(listOf("recents"), sink.calls)
    }

    @Test
    fun trainingCompletionUsesRequiredDetections() {
        assertFalse(GestureTraining.isComplete(1))
        assertTrue(GestureTraining.isComplete(GestureTraining.REQUIRED_DETECTIONS))
        assertEquals(GestureMappingCatalog.CLICK, GestureTraining.nextIncomplete(emptySet()))
        assertEquals(
            GestureMappingCatalog.SCROLL_UP,
            GestureTraining.nextIncomplete(setOf(GestureMappingCatalog.CLICK)),
        )
        assertEquals(
            null,
            GestureTraining.nextIncomplete(GestureMappingCatalog.gestureKeys.toSet()),
        )
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
