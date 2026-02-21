package com.mintanable.notethepad.feature_settings.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.components.SettingItem
import com.mintanable.notethepad.feature_settings.presentation.components.SettingRadioGroup
import com.mintanable.notethepad.feature_settings.presentation.components.SettingSwitchItem
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    currentSettings: Settings,
    isProcessing: Boolean,
    onThemeChanged: (ThemeMode) -> Unit,
    onBackupSettingsChanged: (Boolean) -> Unit,
    showToast: (String) -> Unit
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
                val isGoogleLinked = currentSettings.googleAccount?.isNotBlank() == true
                SettingSwitchItem(
                    "Backup on Google Drive",
                    currentSettings.backupEnabled,
                    true) { checked ->
                    if (!isGoogleLinked && checked) {
                        showToast("Please sign in with Google to enable backups")
                    } else {
                        onBackupSettingsChanged(checked)
                    }
                }
                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if(currentSettings.backupEnabled && currentSettings.googleAccount?.isNotBlank() == true) {
                item {
                    SettingItem(
                        "Google Account",
                        currentSettings.googleAccount
                    ) { }
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
            onThemeChanged = {},
            showToast = {},
            isProcessing = false
        )
    }
}

