package com.mulkallah.aircontrole.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.mulkallah.aircontrole.core.model.CursorPosition

class CursorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var cursor = CursorPosition(0.5f, 0.5f, visible = false)
    private var sizeScale = 1f
    private var pulseEnabled = true
    private var pulseRadius = 0f
    private var pulseAlpha = 0
    private var label: String? = null
    private var animator: ValueAnimator? = null

    fun setCursor(position: CursorPosition) {
        cursor = position
        invalidate()
    }

    fun setSizeScale(scale: Float) {
        sizeScale = scale.coerceIn(0.6f, 2f)
        invalidate()
    }

    fun setPulseEnabled(enabled: Boolean) {
        pulseEnabled = enabled
    }

    fun pulse(gestureLabel: String? = null) {
        label = gestureLabel
        if (!pulseEnabled) {
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 420L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val t = animation.animatedValue as Float
                pulseRadius = 18f * sizeScale + t * 36f * sizeScale
                pulseAlpha = ((1f - t) * 200f).toInt()
                invalidate()
            }
            start()
        }
    }

    fun clearLabel() {
        label = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!cursor.visible) return
        val cx = cursor.x * width
        val cy = cursor.y * height
        val radius = 16f * sizeScale
        if (pulseAlpha > 0) {
            ringPaint.alpha = pulseAlpha
            canvas.drawCircle(cx, cy, pulseRadius, ringPaint)
        }
        fillPaint.alpha = 230
        canvas.drawCircle(cx, cy, radius, fillPaint)
        fillPaint.alpha = 90
        canvas.drawCircle(cx, cy, radius + 8f * sizeScale, fillPaint)
        label?.let { text ->
            canvas.drawText(text, cx, cy - 28f * sizeScale, labelPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
