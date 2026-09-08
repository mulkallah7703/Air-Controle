package com.mulkallah.aircontrole.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineStatusTest {
    @Test
    fun successTokensAreNotErrors() {
        assertFalse(PipelineStatus.isError(PipelineStatus.RUNNING))
        assertFalse(PipelineStatus.isError(PipelineStatus.CAMERA_BOUND))
        assertFalse(PipelineStatus.isError(PipelineStatus.LANDMARKER_READY))
        assertFalse(PipelineStatus.isError(PipelineStatus.ACCESSIBILITY_CONNECTED))
        assertFalse(PipelineStatus.isError(""))
    }

    @Test
    fun failureTokensAreErrors() {
        assertTrue(PipelineStatus.isError(PipelineStatus.MODEL_MISSING))
        assertTrue(PipelineStatus.isError(PipelineStatus.LANDMARKER_FAILED))
        assertTrue(PipelineStatus.isError(PipelineStatus.CAMERA_NO_FRAMES))
        assertTrue(PipelineStatus.isError(PipelineStatus.OVERLAY_FAILED))
    }
}
