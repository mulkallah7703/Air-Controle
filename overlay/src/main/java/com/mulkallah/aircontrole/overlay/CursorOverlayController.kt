package com.mulkallah.aircontrole.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.model.CursorPosition

class CursorOverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: CursorOverlayView? = null
    private var attached = false

    fun show(sizeScale: Float, pulseEnabled: Boolean): Boolean {
        if (attached) {
            view?.setSizeScale(sizeScale)
            view?.setPulseEnabled(pulseEnabled)
            return true
        }
        val overlay = CursorOverlayView(context).apply {
            setSizeScale(sizeScale)
            setPulseEnabled(pulseEnabled)
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "Air Controle cursor"
        }
        return try {
            windowManager.addView(overlay, params)
            view = overlay
            attached = true
            AirControleLog.i("overlay attached")
            true
        } catch (error: Throwable) {
            AirControleLog.e("overlay attach failed", error)
            false
        }
    }

    fun update(position: CursorPosition) {
        view?.setCursor(position)
    }

    fun pulse(label: String?) {
        view?.pulse(label)
    }

    fun hide() {
        val overlay = view ?: return
        if (attached) {
            try {
                windowManager.removeView(overlay)
            } catch (error: Throwable) {
                AirControleLog.w("overlay detach failed", error)
            }
        }
        view = null
        attached = false
        AirControleLog.i("overlay hidden")
    }
}
