package com.mulkallah.aircontrole.control

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mulkallah.aircontrole.core.AirControleConstants
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.model.PipelineStatus
import com.mulkallah.aircontrole.core.permissions.PermissionChecker

/**
 * Starts [AirControlForegroundService] from a visible Activity whenever possible.
 * Application / Accessibility process start is not a valid camera-FGS state on
 * Android 12+ / Samsung One UI.
 */
object AirControlStarter {
    fun start(context: Context): Boolean {
        val camera = PermissionChecker.hasCamera(context)
        AirControleLog.i(
            "FGS start requested ctx=${context.javaClass.name} " +
                "camera=$camera alreadyRunning=${AirControlForegroundService.isStarted}",
        )
        if (!camera) {
            AirControleLog.e("FGS start aborted: CAMERA permission missing")
            AirControlBridge.updateStatus(PipelineStatus.CAMERA_PERMISSION)
            return false
        }
        val intent = Intent(context, AirControlForegroundService::class.java)
            .setAction(AirControleConstants.ACTION_START)
        return try {
            ContextCompat.startForegroundService(context, intent)
            AirControleLog.i("FGS startForegroundService dispatched")
            true
        } catch (first: Throwable) {
            AirControleLog.e("FGS start with ${context.javaClass.simpleName} failed", first)
            try {
                ContextCompat.startForegroundService(context.applicationContext, intent)
                AirControleLog.i("FGS startForegroundService dispatched via applicationContext")
                true
            } catch (second: Throwable) {
                AirControleLog.e("FGS start failed", second)
                AirControlBridge.updateStatus(PipelineStatus.SERVICE_START_FAILED)
                false
            }
        }
    }

    fun stop(context: Context) {
        AirControleLog.i("FGS stop requested ctx=${context.javaClass.name}")
        AirControlForegroundService.stop(context)
    }
}
