package com.aistudio.retroplay.wmpkqa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistudio.retroplay.wmpkqa.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var isDarkMode by remember { mutableStateOf(true) }
    var scaleResolution by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Appearance Category
            SettingsCategory(
                title = stringResource(R.string.settings_theme),
                icon = { Icon(Icons.Default.ColorLens, contentDescription = null) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.settings_theme_dark))
                    Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it })
                }
            }

            HorizontalDivider()

            // Controls Category
            SettingsCategory(
                title = stringResource(R.string.settings_controls),
                icon = { Icon(Icons.Default.Gamepad, contentDescription = null) }
            ) {
                TextButton(onClick = { /* TODO */ }) {
                    Text(stringResource(R.string.settings_controls_customize))
                }
                TextButton(onClick = { /* TODO */ }) {
                    Text(stringResource(R.string.settings_gamepad))
                }
            }

            HorizontalDivider()

            // Graphics Category
            SettingsCategory(
                title = stringResource(R.string.settings_graphics),
                icon = { Icon(Icons.Default.DisplaySettings, contentDescription = null) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.settings_graphics_resolution))
                    Switch(checked = scaleResolution, onCheckedChange = { scaleResolution = it })
                }
                TextButton(onClick = { /* TODO */ }) {
                    Text(stringResource(R.string.settings_graphics_aspect_ratio))
                }
            }
        }
    }
}

@Composable
fun SettingsCategory(
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.padding(end = 16.dp)) { icon() }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.padding(start = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}
