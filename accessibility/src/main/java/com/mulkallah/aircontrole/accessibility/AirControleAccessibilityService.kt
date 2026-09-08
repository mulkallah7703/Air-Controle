package com.mulkallah.aircontrole.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.bridge.AirControlBridge.ScrollDirection
import com.mulkallah.aircontrole.core.bridge.AirControlBridge.SwipeDirection
import com.mulkallah.aircontrole.core.model.PipelineStatus

class AirControleAccessibilityService : AccessibilityService(), AirControlBridge.ActionSink {

    private val mainHandler = Handler(Looper.getMainLooper())

    override val connected: Boolean get() = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        AirControlBridge.actionSink = this
        AirControlBridge.updateStatus(PipelineStatus.ACCESSIBILITY_CONNECTED)
        AirControleLog.i("accessibility connected yes")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (AirControlBridge.actionSink === this) {
            AirControlBridge.actionSink = null
            AirControleLog.i("accessibility connected no")
        }
        super.onDestroy()
    }

    override fun performHome(): Boolean = onMain {
        val ok = performGlobalAction(GLOBAL_ACTION_HOME)
        AirControleLog.i("accessibility HOME result=$ok")
        ok
    }

    override fun performBack(): Boolean = onMain {
        val ok = performGlobalAction(GLOBAL_ACTION_BACK)
        AirControleLog.i("accessibility BACK result=$ok")
        ok
    }

    override fun performRecents(): Boolean = onMain {
        val ok = performGlobalAction(GLOBAL_ACTION_RECENTS)
        AirControleLog.i("accessibility RECENTS result=$ok")
        ok
    }

    override fun performClick(x: Float, y: Float): Boolean = onMain {
        val (width, height) = screenSize()
        val px = (x * width).coerceIn(0f, width)
        val py = (y * height).coerceIn(0f, height)
        val path = Path().apply { moveTo(px, py) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 60)
        val ok = dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        AirControleLog.i("accessibility CLICK result=$ok at=$px,$py")
        ok
    }

    override fun performScroll(direction: ScrollDirection): Boolean = onMain {
        val (width, height) = screenSize()
        val x = width * 0.5f
        val startY: Float
        val endY: Float
        when (direction) {
            ScrollDirection.UP -> {
                startY = height * 0.68f
                endY = height * 0.32f
            }
            ScrollDirection.DOWN -> {
                startY = height * 0.32f
                endY = height * 0.68f
            }
        }
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 280)
        val ok = dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        AirControleLog.i("accessibility SCROLL ${direction.name} result=$ok")
        ok
    }

    override fun performSwipe(direction: SwipeDirection): Boolean = onMain {
        val (width, height) = screenSize()
        val y = height * 0.5f
        val startX: Float
        val endX: Float
        when (direction) {
            SwipeDirection.LEFT -> {
                startX = width * 0.78f
                endX = width * 0.22f
            }
            SwipeDirection.RIGHT -> {
                startX = width * 0.22f
                endX = width * 0.78f
            }
        }
        val path = Path().apply {
            moveTo(startX, y)
            lineTo(endX, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 280)
        val ok = dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        AirControleLog.i("accessibility SWIPE ${direction.name} result=$ok")
        ok
    }

    override fun openApplication(packageName: String): Boolean {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(launch)
            true
        } catch (_: Exception) {
            rootInActiveWindow?.let { node ->
                findAndClickByPackage(node, packageName)
            } ?: false
        }
    }

    private fun findAndClickByPackage(node: AccessibilityNodeInfo, packageName: String): Boolean {
        if (node.packageName?.toString() == packageName && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickByPackage(child, packageName)) return true
        }
        return false
    }

    private fun screenSize(): Pair<Float, Float> {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width().toFloat() to bounds.height().toFloat()
        } else {
            val metrics = resources.displayMetrics
            metrics.widthPixels.toFloat() to metrics.heightPixels.toFloat()
        }
    }

    private fun onMain(block: () -> Boolean): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }
        mainHandler.post { block() }
        return true
    }
}
