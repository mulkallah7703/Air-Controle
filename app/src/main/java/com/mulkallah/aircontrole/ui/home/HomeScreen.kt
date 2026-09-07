package com.mulkallah.aircontrole.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsHandball
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.control.AirControlForegroundService
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.model.QuickAccessApp
import com.mulkallah.aircontrole.core.permissions.PermissionChecker
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.ui.theme.AirCyan
import com.mulkallah.aircontrole.ui.theme.AirNavy
import com.mulkallah.aircontrole.ui.theme.AirOk
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    preferences: AirControlePreferences,
    onSettings: () -> Unit,
    onTraining: () -> Unit,
    onCustomize: () -> Unit,
    onAddApp: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by preferences.airControlEnabled.collectAsState(initial = false)
    val extras by preferences.extraApps.collectAsState(initial = emptySet())
    val running by AirControlBridge.running.collectAsState()
    val paused by AirControlBridge.paused.collectAsState()
    val machine by AirControlBridge.machineState.collectAsState()
    val lastGesture by AirControlBridge.lastGesture.collectAsState()
    var permissions by remember { mutableStateOf(PermissionChecker.snapshot(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = PermissionChecker.snapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val apps = remember(extras) { preferences.resolveQuickAccess(extras) }
    val on = enabled || running

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirNavy)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Air Controle", style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.tagline), color = AirCyan, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.home_settings))
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(stringResource(R.string.home_air_control), style = MaterialTheme.typography.titleLarge)
                            val statusText = when {
                                paused -> stringResource(R.string.home_paused)
                                on -> stringResource(R.string.home_on)
                                else -> stringResource(R.string.home_off)
                            }
                            Text(
                                text = statusText,
                                color = if (on && !paused) AirOk else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Switch(
                            checked = on,
                            onCheckedChange = { checked ->
                                if (checked && !permissions.readyForAirControl) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.home_missing_permissions),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    return@Switch
                                }
                                scope.launch {
                                    preferences.setAirControlEnabled(checked)
                                    if (checked) {
                                        AirControlForegroundService.start(context)
                                    } else {
                                        AirControlForegroundService.stop(context)
                                    }
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${stringResource(R.string.home_status)} · ${localizedState(machine)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    lastGesture?.let { gesture ->
                        Text(
                            text = "${stringResource(R.string.home_last_gesture)} · ${localizedGesture(gesture)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AirCyan,
                        )
                    }
                    if (on && !permissions.accessibility) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.home_accessibility_hint), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (on) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_running_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.home_quick_access), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(280.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(apps, key = { it.id }) { app ->
                    QuickAccessTile(app) {
                        val launched = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launched != null) {
                            launched.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launched)
                            AirControlBridge.actionSink?.openApplication(app.packageName)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.home_not_installed, app.label),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
                item {
                    QuickAccessTile(
                        app = QuickAccessApp("add", stringResource(R.string.home_add_app), ""),
                        icon = Icons.Outlined.Add,
                        onClick = onAddApp,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onTraining) {
                    Icon(Icons.Outlined.SportsHandball, contentDescription = null)
                    Text(stringResource(R.string.home_training), modifier = Modifier.padding(start = 8.dp))
                }
                TextButton(onClick = onCustomize) {
                    Icon(Icons.Outlined.Tune, contentDescription = null)
                    Text(stringResource(R.string.home_customize), modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickAccessTile(
    app: QuickAccessApp,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Apps,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .height(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AirCyan.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = app.label, tint = AirCyan)
        }
        Spacer(Modifier.height(8.dp))
        Text(app.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun localizedState(state: String): String {
    val res = when (state) {
        "IDLE" -> R.string.state_idle
        "HAND_DETECTED" -> R.string.state_hand_detected
        "TRACKING" -> R.string.state_tracking
        "GESTURE_RECOGNIZED" -> R.string.state_gesture_recognized
        "ACTION" -> R.string.state_action
        "COOLDOWN" -> R.string.state_cooldown
        else -> R.string.state_idle
    }
    return stringResource(res)
}

@Composable
private fun localizedGesture(gesture: String): String {
    val res = when (gesture) {
        "CLICK" -> R.string.gesture_click
        "SCROLL_UP" -> R.string.gesture_scroll_up
        "SCROLL_DOWN" -> R.string.gesture_scroll_down
        "SWIPE_LEFT" -> R.string.gesture_swipe_left
        "SWIPE_RIGHT" -> R.string.gesture_swipe_right
        "PALM_PAUSE" -> R.string.gesture_palm
        "FIST_BACK" -> R.string.gesture_fist
        "PEACE_HOME" -> R.string.gesture_peace
        "POINT_MOVE" -> R.string.gesture_point
        else -> R.string.gesture_point
    }
    return stringResource(res)
}
