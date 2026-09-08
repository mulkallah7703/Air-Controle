package com.mulkallah.aircontrole.core.model

import org.junit.Assert.assertEquals
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
        assertTrue(PipelineStatus.isError(PipelineStatus.SERVICE_START_FAILED))
        assertTrue(PipelineStatus.isError(PipelineStatus.SERVICE_NOT_RUNNING))
        assertTrue(PipelineStatus.isError(PipelineStatus.CAMERA_START_FAILED))
        assertTrue(PipelineStatus.isError(PipelineStatus.CAMERA_ERROR))
    }

    @Test
    fun fromThrowableMapsCameraXSurfaceCombination() {
        val error = IllegalArgumentException("No supported surface combination is found for camera device")
        assertEquals(PipelineStatus.CAMERA_START_FAILED, PipelineStatus.fromThrowable(error))
    }

    @Test
    fun fromThrowableMapsBindFailureCauseChain() {
        val cause = RuntimeException("CameraUnavailableException: Camera is in use")
        val error = IllegalStateException("Use case binding failed", cause)
        assertEquals(PipelineStatus.CAMERA_START_FAILED, PipelineStatus.fromThrowable(error))
    }

    @Test
    fun fromThrowableMapsNativeLandmarkerLoad() {
        val error = UnsatisfiedLinkError("dlopen failed: libmediapipe.so")
        assertEquals(PipelineStatus.LANDMARKER_FAILED, PipelineStatus.fromThrowable(error))
    }

    @Test
    fun fromThrowableMapsModelMissingToken() {
        val error = IllegalStateException(PipelineStatus.MODEL_MISSING)
        assertEquals(PipelineStatus.MODEL_MISSING, PipelineStatus.fromThrowable(error))
    }

    @Test
    fun fromThrowableFallsBackToCameraError() {
        val error = RuntimeException("something unexpected")
        assertEquals(PipelineStatus.CAMERA_ERROR, PipelineStatus.fromThrowable(error))
    }
}
