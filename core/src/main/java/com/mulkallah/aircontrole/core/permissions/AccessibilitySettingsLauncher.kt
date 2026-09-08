package com.mulkallah.aircontrole.core.permissions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.mulkallah.aircontrole.core.AirControleConstants

/**
 * Opens system Accessibility settings, preferring a highlight on Air Controle.
 *
 * Target component (document this for support / Play reviews):
 * `com.mulkallah.aircontrole` /
 * `com.mulkallah.aircontrole.accessibility.AirControleAccessibilityService`
 *
 * Uses [Settings.ACTION_ACCESSIBILITY_SETTINGS] plus AOSP Settings extras
 * (`:settings:fragment_args_key`, `:settings:show_fragment_args`) so stock
 * Settings can scroll to / expand that service. OEMs may ignore the extras
 * and show the full list — there is no public API that guarantees focus.
 */
object AccessibilitySettingsLauncher {
    fun open(context: Context) {
        val intent = buildHighlightIntent()
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            if (context !is Activity) {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun buildHighlightIntent(): Intent {
        val component = ComponentName(
            AirControleConstants.APPLICATION_ID,
            AirControleConstants.ACCESSIBILITY_SERVICE_CLASS,
        )
        val flattened = component.flattenToString()
        val args = Bundle().apply {
            putString(FRAGMENT_ARG_KEY, flattened)
        }
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, component)
            putExtra(FRAGMENT_ARG_KEY, flattened)
            putExtra(SHOW_FRAGMENT_ARGS, args)
        }
    }

    private const val FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"
}

object PermissionSettingsLauncher {
    fun openOverlay(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        start(context, intent)
    }

    fun openNotifications(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        start(context, intent)
    }

    fun openAppDetails(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
        start(context, intent)
    }

    private fun start(context: Context, intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
