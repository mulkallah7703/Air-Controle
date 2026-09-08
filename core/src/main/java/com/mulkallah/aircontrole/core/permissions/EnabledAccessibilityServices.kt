package com.mulkallah.aircontrole.core.permissions

import com.mulkallah.aircontrole.core.AirControleConstants

/**
 * Source of truth for whether the user turned on Air Controle in system
 * Accessibility settings. Parses [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES]
 * (`:`-separated flattened [android.content.ComponentName] values).
 *
 * Do **not** use [android.view.accessibility.AccessibilityManager] for Ready —
 * after reinstalls OEMs (Samsung) can keep DEAD bindings that make the manager
 * list look enabled while the secure setting is still null.
 */
object EnabledAccessibilityServices {
    val flattened: String =
        "${AirControleConstants.APPLICATION_ID}/${AirControleConstants.ACCESSIBILITY_SERVICE_CLASS}"

    val shortFlattened: String =
        "${AirControleConstants.APPLICATION_ID}/.accessibility.AirControleAccessibilityService"

    fun isAirControleEnabled(rawSetting: String?): Boolean {
        if (rawSetting.isNullOrBlank()) return false
        if (rawSetting.equals("null", ignoreCase = true)) return false
        return rawSetting.split(':').any { token -> matchesAirControle(token.trim()) }
    }

    fun matchesAirControle(component: String): Boolean {
        if (component.isEmpty()) return false
        if (component == flattened || component == shortFlattened) return true
        val slash = component.indexOf('/')
        if (slash <= 0 || slash == component.lastIndex) return false
        val pkg = component.substring(0, slash)
        if (pkg != AirControleConstants.APPLICATION_ID) return false
        val cls = component.substring(slash + 1)
        val fullClass = if (cls.startsWith('.')) pkg + cls else cls
        return fullClass == AirControleConstants.ACCESSIBILITY_SERVICE_CLASS
    }
}
