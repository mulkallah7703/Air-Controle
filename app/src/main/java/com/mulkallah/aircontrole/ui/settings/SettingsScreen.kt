package com.mulkallah.aircontrole.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mulkallah.aircontrole.BuildConfig
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.control.AirControlForegroundService
import com.mulkallah.aircontrole.core.AirControleConstants
import com.mulkallah.aircontrole.core.locale.LocaleController
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.ui.theme.AirDanger
import com.mulkallah.aircontrole.ui.theme.AirNavy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AirControlePreferences,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by preferences.languageTag.collectAsState(initial = "ar")
    val cursorSize by preferences.cursorSize.collectAsState(initial = 1f)
    val pulse by preferences.cursorPulse.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirNavy),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = language == "ar",
                        onClick = {
                            scope.launch {
                                preferences.setLanguageTag("ar")
                                LocaleController.apply("ar")
                            }
                        },
                        label = { Text(stringResource(R.string.settings_language_ar)) },
                    )
                    FilterChip(
                        selected = language == "en",
                        onClick = {
                            scope.launch {
                                preferences.setLanguageTag("en")
                                LocaleController.apply("en")
                            }
                        },
                        label = { Text(stringResource(R.string.settings_language_en)) },
                    )
                }
            }

            SettingsCard(title = stringResource(R.string.settings_cursor)) {
                Text(stringResource(R.string.settings_cursor_size), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = cursorSize,
                    onValueChange = { value -> scope.launch { preferences.setCursorSize(value) } },
                    valueRange = 0.6f..2f,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.settings_cursor_pulse))
                    Switch(
                        checked = pulse,
                        onCheckedChange = { checked -> scope.launch { preferences.setCursorPulse(checked) } },
                    )
                }
            }

            SettingsCard(title = stringResource(R.string.settings_kill_switch)) {
                Text(
                    text = stringResource(R.string.settings_kill_switch_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch { preferences.setAirControlEnabled(false) }
                        AirControlForegroundService.stop(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AirDanger),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_kill_now))
                }
            }

            SettingsCard(title = stringResource(R.string.settings_purchase)) {
                Text(
                    text = stringResource(R.string.settings_purchase_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_purchase_price),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            SettingsCard(title = stringResource(R.string.settings_limits_title)) {
                Text(
                    text = stringResource(R.string.settings_limits_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${AirControleConstants.PURCHASE_PRICE_SAR} SAR",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
