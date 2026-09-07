package com.mulkallah.aircontrole.gestures

import com.mulkallah.aircontrole.core.model.CursorPosition

data class GestureFrameResult(
    val state: GestureState,
    val pose: HandPose,
    val recognized: GestureType,
    val cursor: CursorPosition,
    val pulse: Boolean,
    val shouldDispatch: Boolean,
)
