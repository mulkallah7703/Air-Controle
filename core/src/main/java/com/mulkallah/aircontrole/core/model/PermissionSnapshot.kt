package com.mulkallah.aircontrole.core.model

data class PermissionSnapshot(
    val camera: Boolean,
    val accessibility: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
) {
    val allGranted: Boolean get() = camera && accessibility && overlay && notifications

    val readyForAirControl: Boolean get() = camera
}
