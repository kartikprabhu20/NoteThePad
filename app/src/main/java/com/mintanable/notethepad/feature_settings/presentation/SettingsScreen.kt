package com.mintanable.notethepad.feature_settings.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.components.SettingRadioGroup
import com.mintanable.notethepad.feature_settings.presentation.components.SettingSwitchItem
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    currentSettings: Settings,
    onThemeChanged: (ThemeMode) -> Unit,
    onBackupSettingsChanged: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge
                    )
                        },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                }
            )
        }
    ) { paddingValue ->


        LazyColumn(modifier =  Modifier
            .fillMaxSize()
            .padding(paddingValue)
            .padding(horizontal = 16.dp)
        ) {

            item {
                SettingSwitchItem("Backup on Google Drive", currentSettings.backupEnabled) {
                    onBackupSettingsChanged(it)
                }
            }

            item {
                SettingRadioGroup(
                    title = "Theme modes",
                    selectedOption = currentSettings.themeMode,
                    onOptionSelected = { onThemeChanged(it) },
                    entries = ThemeMode.entries
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewSettingsScreen(

) {
    NoteThePadTheme {
        SettingsScreen(
            onBackPressed = {},
            currentSettings = Settings(),
            onBackupSettingsChanged = {},
            onThemeChanged = {}
        )
    }
}

