package com.mulkallah.aircontrole.core.model

data class NormalizedPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
) {
    fun distanceTo(other: NormalizedPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
