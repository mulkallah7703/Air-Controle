package com.mulkallah.aircontrole.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.core.model.PermissionSnapshot
import com.mulkallah.aircontrole.core.permissions.AccessibilitySettingsLauncher
import com.mulkallah.aircontrole.core.permissions.PermissionSettingsLauncher
import com.mulkallah.aircontrole.ui.theme.AirCyan
import com.mulkallah.aircontrole.ui.theme.AirOk

@Composable
fun PermissionChecklist(
    permissions: PermissionSnapshot,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }
    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.home_permissions_title), style = MaterialTheme.typography.titleMedium)
            PermissionRow(
                icon = Icons.Outlined.PhotoCamera,
                label = stringResource(R.string.perm_camera),
                granted = permissions.camera,
                onClick = {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                },
            )
            PermissionRow(
                icon = Icons.Outlined.AccessibilityNew,
                label = stringResource(R.string.perm_accessibility),
                granted = permissions.accessibility,
                offLabel = stringResource(R.string.perm_status_accessibility_off),
                onClick = { AccessibilitySettingsLauncher.open(context) },
            )
            PermissionRow(
                icon = Icons.Outlined.Layers,
                label = stringResource(R.string.perm_overlay),
                granted = permissions.overlay,
                onClick = { PermissionSettingsLauncher.openOverlay(context) },
            )
            PermissionRow(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.perm_notifications),
                granted = permissions.notifications,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.notifications) {
                        notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionSettingsLauncher.openNotifications(context)
                    }
                },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
    offLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (granted) AirOk else AirCyan)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when {
                    granted -> stringResource(R.string.perm_status_on)
                    offLabel != null -> offLabel
                    else -> stringResource(R.string.perm_status_off)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (granted) AirOk else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = stringResource(R.string.action_open_settings),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
