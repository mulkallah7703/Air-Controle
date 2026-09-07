package com.mulkallah.aircontrole.ui.stubs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.ui.theme.AirCyan
import com.mulkallah.aircontrole.ui.theme.AirNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    training: Boolean,
    onBack: () -> Unit,
) {
    val title = if (training) R.string.coming_soon_training_title else R.string.coming_soon_customize_title
    val body = if (training) R.string.coming_soon_training_body else R.string.coming_soon_customize_body
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirNavy),
    ) {
        TopAppBar(
            title = { Text(stringResource(title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AirNavy),
        )
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.coming_soon_later), color = AirCyan, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
