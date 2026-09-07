package com.mulkallah.aircontrole.core.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.mulkallah.aircontrole.core.AirControleConstants
import com.mulkallah.aircontrole.core.model.PermissionSnapshot

object PermissionChecker {
    fun snapshot(context: Context): PermissionSnapshot {
        return PermissionSnapshot(
            camera = hasCamera(context),
            accessibility = hasAccessibility(context),
            overlay = hasOverlay(context),
            notifications = hasNotifications(context),
        )
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
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val expected = "${AirControleConstants.APPLICATION_ID}/${AirControleConstants.ACCESSIBILITY_SERVICE_CLASS}"
        val shortExpected = AirControleConstants.ACCESSIBILITY_SERVICE_CLASS
        return enabled.any { info ->
            val id = info.resolveInfo?.serviceInfo?.let { service ->
                "${service.packageName}/${service.name}"
            } ?: info.id
            id == expected || id.endsWith(shortExpected) || info.id.contains("AirControleAccessibilityService")
        }
    }
}
