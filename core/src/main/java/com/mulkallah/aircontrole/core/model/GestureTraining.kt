package com.mulkallah.aircontrole.core.model

object GestureTraining {
    const val REQUIRED_DETECTIONS = 2
    const val POINT_HOLD_MS = 800L
    const val POINT_REARM_MS = 900L

    fun isComplete(count: Int): Boolean = count >= REQUIRED_DETECTIONS

    fun nextIncomplete(trained: Set<String>): String? {
        return GestureMappingCatalog.gestureKeys.firstOrNull { it !in trained }
    }
}
