package com.mulkallah.aircontrole.core

object AirControleConstants {
    const val APPLICATION_ID = "com.mulkallah.aircontrole"
    const val ACCESSIBILITY_SERVICE_CLASS =
        "com.mulkallah.aircontrole.accessibility.AirControleAccessibilityService"
    const val FOREGROUND_SERVICE_CLASS =
        "com.mulkallah.aircontrole.control.AirControlForegroundService"

    const val NOTIFICATION_CHANNEL_ID = "air_controle_active"
    const val NOTIFICATION_ID = 1101

    const val PURCHASE_PRICE_SAR = "29.99"
    const val DEFAULT_LANGUAGE_TAG = "ar"

    const val ACTION_START = "com.mulkallah.aircontrole.action.START"
    const val ACTION_STOP = "com.mulkallah.aircontrole.action.STOP"
    const val ACTION_KILL_SWITCH = "com.mulkallah.aircontrole.action.KILL_SWITCH"
}
