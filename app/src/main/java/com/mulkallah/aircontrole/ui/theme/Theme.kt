package com.mulkallah.aircontrole.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val AirColorScheme = darkColorScheme(
    primary = AirCyan,
    onPrimary = AirNavy,
    secondary = AirCyanDim,
    onSecondary = AirInk,
    background = AirNavy,
    onBackground = AirInk,
    surface = AirNavyRaised,
    onSurface = AirInk,
    surfaceVariant = AirNavyRaised,
    onSurfaceVariant = AirMuted,
    error = AirDanger,
    onError = AirInk,
    outline = AirMuted,
)

@Composable
fun AirControleTheme(
    rtl: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = AirColorScheme,
            typography = AirTypography,
            content = content,
        )
    }
}
