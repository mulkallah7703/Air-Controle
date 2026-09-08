package com.mulkallah.aircontrole.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.model.PermissionSnapshot

object PermissionChecker {
    fun snapshot(context: Context): PermissionSnapshot {
        val rawAccessibility = enabledAccessibilityServicesSetting(context)
        val accessibility = EnabledAccessibilityServices.isAirControleEnabled(rawAccessibility)
        val snapshot = PermissionSnapshot(
            camera = hasCamera(context),
            accessibility = accessibility,
            overlay = hasOverlay(context),
            notifications = hasNotifications(context),
        )
        AirControleLog.i(
            "permissions camera=${snapshot.camera} overlay=${snapshot.overlay} " +
                "notifications=${snapshot.notifications} " +
                "accessibilityReady=${snapshot.accessibility} " +
                "accessibilitySetting=${rawAccessibility ?: "null"} " +
                "accessibilityConnected=${AirControlBridge.accessibilityConnected} " +
                "expected=${EnabledAccessibilityServices.flattened}",
        )
        return snapshot
    }

    fun hasCamera(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAccessibility(context: Context): Boolean {
        return EnabledAccessibilityServices.isAirControleEnabled(
            enabledAccessibilityServicesSetting(context),
        )
    }

    fun enabledAccessibilityServicesSetting(context: Context): String? {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
    }
}
