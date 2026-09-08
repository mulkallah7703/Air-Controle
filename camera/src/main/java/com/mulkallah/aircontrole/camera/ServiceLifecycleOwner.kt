package com.mulkallah.aircontrole.camera

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mulkallah.aircontrole.core.AirControleLog

/**
 * CameraX [ProcessCameraProvider.bindToLifecycle] needs a lifecycle that is at
 * least STARTED. [androidx.lifecycle.LifecycleService] never reaches RESUMED,
 * and Samsung CameraX often refuses to keep ImageAnalysis open in that state.
 * This owner is advanced to RESUMED on the main thread before bind.
 */
class ServiceLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    fun start() {
        if (registry.currentState == Lifecycle.State.DESTROYED) {
            throw IllegalStateException("ServiceLifecycleOwner cannot restart after DESTROYED")
        }
        if (registry.currentState == Lifecycle.State.INITIALIZED) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
        if (registry.currentState == Lifecycle.State.CREATED) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (registry.currentState == Lifecycle.State.STARTED) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        AirControleLog.i("camera lifecycle -> ${registry.currentState}")
    }

    fun stop() {
        if (registry.currentState == Lifecycle.State.INITIALIZED ||
            registry.currentState == Lifecycle.State.DESTROYED
        ) {
            return
        }
        if (registry.currentState == Lifecycle.State.RESUMED) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (registry.currentState == Lifecycle.State.STARTED) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        if (registry.currentState == Lifecycle.State.CREATED) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        AirControleLog.i("camera lifecycle -> ${registry.currentState}")
    }
}
