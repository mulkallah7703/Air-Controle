package com.mulkallah.aircontrole.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.SportsMma
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.core.model.GestureMappingCatalog
import com.mulkallah.aircontrole.ui.gestures.gestureGuideBodyRes
import com.mulkallah.aircontrole.ui.gestures.gestureTitleRes
import com.mulkallah.aircontrole.ui.theme.AirCyan
import com.mulkallah.aircontrole.ui.theme.AirNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureGuideScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirNavy),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.guide_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AirNavy),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.guide_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GestureMappingCatalog.guideKeys.forEach { key ->
                GuideRow(
                    icon = gestureIcon(key),
                    title = stringResource(gestureTitleRes(key)),
                    body = stringResource(gestureGuideBodyRes(key)),
                )
            }
        }
    }
}

@Composable
private fun GuideRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AirCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = title, tint = AirCyan, modifier = Modifier.size(28.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun gestureIcon(key: String): ImageVector = when (key) {
    GestureMappingCatalog.HAND_START -> Icons.Outlined.WavingHand
    GestureMappingCatalog.POINT_MOVE -> Icons.Outlined.TouchApp
    GestureMappingCatalog.CLICK -> Icons.Outlined.AdsClick
    GestureMappingCatalog.SCROLL_UP -> Icons.Outlined.KeyboardDoubleArrowUp
    GestureMappingCatalog.SCROLL_DOWN -> Icons.Outlined.KeyboardDoubleArrowDown
    GestureMappingCatalog.SWIPE_RIGHT -> Icons.Outlined.SwipeRight
    GestureMappingCatalog.SWIPE_LEFT -> Icons.Outlined.SwipeLeft
    GestureMappingCatalog.PALM_PAUSE -> Icons.Outlined.PauseCircle
    GestureMappingCatalog.FIST_BACK -> Icons.Outlined.SportsMma
    GestureMappingCatalog.PEACE_HOME -> Icons.Outlined.Home
    else -> Icons.Outlined.PanTool
}
