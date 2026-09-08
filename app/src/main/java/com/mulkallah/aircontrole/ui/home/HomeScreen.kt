package com.mulkallah.aircontrole.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.control.AirControlForegroundService
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.model.PipelineStatus
import com.mulkallah.aircontrole.core.model.QuickAccessApp
import com.mulkallah.aircontrole.core.permissions.AccessibilitySettingsLauncher
import com.mulkallah.aircontrole.core.permissions.PermissionChecker
import com.mulkallah.aircontrole.core.permissions.PermissionSettingsLauncher
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.ui.gestures.gestureShortRes
import com.mulkallah.aircontrole.ui.gestures.machineStateRes
import com.mulkallah.aircontrole.ui.gestures.pipelineErrorRes
import com.mulkallah.aircontrole.ui.theme.AirCyan
import com.mulkallah.aircontrole.ui.theme.AirDanger
import com.mulkallah.aircontrole.ui.theme.AirNavy
import com.mulkallah.aircontrole.ui.theme.AirOk
import kotlinx.coroutines.launch

private data class QuickAccessVisual(
    val app: QuickAccessApp,
    val label: String,
    val installed: Boolean,
    val icon: Drawable?,
)

@Composable
fun HomeScreen(
    preferences: AirControlePreferences,
    onSettings: () -> Unit,
    onGestureGuide: () -> Unit,
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
    val pipelineStatus by AirControlBridge.statusMessage.collectAsState()
    var permissions by remember { mutableStateOf(PermissionChecker.snapshot(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, enabled, running) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = PermissionChecker.snapshot(context)
                if (enabled && permissions.readyForAirControl && !running) {
                    AirControlForegroundService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val apps = remember(extras) { preferences.resolveQuickAccess(extras) }
    val visuals = remember(apps) { apps.map { resolveQuickAccessVisual(context.packageManager, it) } }
    val on = enabled || running
    val tracking = machine == "HAND_DETECTED" || machine == "TRACKING" ||
        machine == "GESTURE_RECOGNIZED" || machine == "ACTION" || machine == "COOLDOWN"

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
                        text = "${stringResource(R.string.home_status)} · ${stringResource(machineStateRes(machine))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (on && tracking && !paused) {
                        Spacer(Modifier.height(8.dp))
                        TrackingChip(machine)
                    }
                    lastGesture?.let { gesture ->
                        Text(
                            text = "${stringResource(R.string.home_last_gesture)} · ${stringResource(gestureShortRes(gesture))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AirCyan,
                        )
                    }
                    if (on && !tracking && !paused && !PipelineStatus.isError(pipelineStatus)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_waiting_hand),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AirCyan,
                        )
                    }
                    val errorRes = pipelineErrorRes(pipelineStatus)
                    if (on && errorRes != null) {
                        Spacer(Modifier.height(12.dp))
                        PipelineErrorCard(
                            message = stringResource(errorRes),
                            onRetry = {
                                scope.launch {
                                    preferences.setAirControlEnabled(true)
                                    AirControlForegroundService.start(context)
                                }
                            },
                        )
                    }
                    if (!permissions.accessibility) {
                        Spacer(Modifier.height(12.dp))
                        AccessibilityWarning(onClick = { AccessibilitySettingsLauncher.open(context) })
                    }
                    if (on) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_running_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_battery_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                PermissionSettingsLauncher.openBatteryOptimization(context)
                            },
                        )
                        TextButton(onClick = { PermissionSettingsLauncher.openBatteryOptimization(context) }) {
                            Text(stringResource(R.string.home_battery_cta))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            PermissionChecklist(
                permissions = permissions,
                onRefresh = { permissions = PermissionChecker.snapshot(context) },
            )
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.home_quick_access), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(visuals, key = { it.app.id }) { visual ->
                    QuickAccessTile(visual) {
                        val launched = context.packageManager.getLaunchIntentForPackage(visual.app.packageName)
                        if (launched != null) {
                            launched.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launched)
                            AirControlBridge.actionSink?.openApplication(visual.app.packageName)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.home_not_installed, visual.label),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
                item {
                    QuickAccessTile(
                        visual = QuickAccessVisual(
                            app = QuickAccessApp("add", stringResource(R.string.home_add_app), ""),
                            label = stringResource(R.string.home_add_app),
                            installed = true,
                            icon = null,
                        ),
                        fallbackIcon = Icons.Outlined.Add,
                        onClick = onAddApp,
                    )
                }
            }

            TextButton(onClick = onGestureGuide) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                Text(stringResource(R.string.home_guide), modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TrackingChip(machine: String) {
    val label = when (machine) {
        "HAND_DETECTED" -> stringResource(R.string.home_control_started)
        else -> stringResource(R.string.home_tracking)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AirCyan.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = AirCyan, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun QuickAccessTile(
    visual: QuickAccessVisual,
    fallbackIcon: ImageVector = Icons.Outlined.Apps,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .height(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AirCyan.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            val drawable = visual.icon
            if (drawable != null) {
                val bitmap = remember(visual.app.packageName, drawable) {
                    drawable.toBitmap(width = 96, height = 96)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = visual.label,
                    modifier = Modifier.size(36.dp),
                )
            } else {
                Icon(fallbackIcon, contentDescription = visual.label, tint = AirCyan)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = visual.label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!visual.installed && visual.app.packageName.isNotEmpty()) {
            Text(
                text = stringResource(R.string.home_app_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PipelineErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AirDanger.copy(alpha = 0.12f))
            .clickable(onClick = onRetry)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.home_pipeline_error_title),
            style = MaterialTheme.typography.titleSmall,
            color = AirDanger,
        )
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(R.string.home_pipeline_retry),
            style = MaterialTheme.typography.labelLarge,
            color = AirCyan,
        )
    }
}

@Composable
private fun AccessibilityWarning(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AirDanger.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = AirDanger)
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_accessibility_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_accessibility_cta),
                style = MaterialTheme.typography.labelLarge,
                color = AirCyan,
            )
            Text(
                text = stringResource(R.string.home_accessibility_package),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = stringResource(R.string.action_open_settings),
            tint = AirCyan,
        )
    }
}

private fun resolveQuickAccessVisual(pm: PackageManager, app: QuickAccessApp): QuickAccessVisual {
    val icon = try {
        pm.getApplicationIcon(app.packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
    val label = try {
        val info = pm.getApplicationInfo(app.packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        app.label
    }
    val installed = icon != null && pm.getLaunchIntentForPackage(app.packageName) != null
    return QuickAccessVisual(
        app = app,
        label = label,
        installed = installed,
        icon = icon,
    )
}
