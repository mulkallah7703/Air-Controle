package com.mulkallah.aircontrole.ui.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.core.model.GestureAction
import com.mulkallah.aircontrole.core.model.GestureMappingCatalog
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.ui.gestures.actionLabelRes
import com.mulkallah.aircontrole.ui.gestures.gestureHowRes
import com.mulkallah.aircontrole.ui.gestures.gestureTitleRes
import com.mulkallah.aircontrole.ui.theme.AirCyan
import com.mulkallah.aircontrole.ui.theme.AirNavy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeGesturesScreen(
    preferences: AirControlePreferences,
    onBack: () -> Unit,
) {
    val mappings by preferences.gestureActions.collectAsState(initial = GestureMappingCatalog.defaults)
    val scope = rememberCoroutineScope()
    var editingKey by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirNavy),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.customize_title)) },
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
                text = stringResource(R.string.customize_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GestureMappingCatalog.gestureKeys.forEach { key ->
                val action = mappings[key] ?: GestureMappingCatalog.defaults.getValue(key)
                MappingRow(
                    title = stringResource(gestureTitleRes(key)),
                    how = stringResource(gestureHowRes(key)),
                    actionLabel = stringResource(actionLabelRes(action)),
                    onClick = { editingKey = key },
                )
            }
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.customize_reset))
            }
        }
    }

    val currentKey = editingKey
    if (currentKey != null) {
        val selected = mappings[currentKey] ?: GestureMappingCatalog.defaults.getValue(currentKey)
        AlertDialog(
            onDismissRequest = { editingKey = null },
            title = { Text(stringResource(gestureTitleRes(currentKey))) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.customize_pick_action),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GestureAction.assignable.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = action == selected,
                                    onClick = {
                                        scope.launch {
                                            preferences.setGestureAction(currentKey, action)
                                            editingKey = null
                                        }
                                    },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = action == selected,
                                onClick = {
                                    scope.launch {
                                        preferences.setGestureAction(currentKey, action)
                                        editingKey = null
                                    }
                                },
                            )
                            Text(
                                text = stringResource(actionLabelRes(action)),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingKey = null }) {
                    Text(stringResource(R.string.action_back))
                }
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.customize_reset_confirm_title)) },
            text = { Text(stringResource(R.string.customize_reset_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            preferences.resetGestureActions()
                            confirmReset = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.customize_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_back))
                }
            },
        )
    }
}

@Composable
private fun MappingRow(
    title: String,
    how: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = how,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${stringResource(R.string.customize_current_action)} · $actionLabel",
                style = MaterialTheme.typography.bodyLarge,
                color = AirCyan,
            )
        }
    }
}
