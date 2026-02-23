package com.mintanable.notethepad.feature_settings.presentation

import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_backup.presentation.BackupUiState
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.components.BackupStatusUI
import com.mintanable.notethepad.feature_settings.presentation.components.RadioButtonsAlertDialog
import com.mintanable.notethepad.feature_settings.presentation.components.SettingItem
import com.mintanable.notethepad.feature_settings.presentation.components.SettingRadioGroup
import com.mintanable.notethepad.feature_settings.presentation.components.SettingSwitchItem
import com.mintanable.notethepad.feature_settings.presentation.components.TimePickerDialog
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    currentSettings: Settings,
    onLoadBackupInfo: () -> Unit,
    backupUploadDownloadState: BackupStatus,
    backupUiState: BackupUiState,
    isAuthorisingBackup: Boolean,
    onThemeChanged: (ThemeMode) -> Unit,
    onBackupSettingsChanged: (Boolean) -> Unit,
    onBackupTimeChanged: (Int, Int) -> Unit,
    onBackupIntervalChanged: (BackupFrequency) -> Unit,
    showToast: (String) -> Unit,
    onBackupNowClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onDummyDataCreate: () -> Unit
) {

    LaunchedEffect(Unit) {
        onLoadBackupInfo()
    }

    var showIntervalDialog by rememberSaveable {  mutableStateOf(false) }
    var showTimePickerDialog by rememberSaveable { mutableStateOf(false) }
    val isGoogleLinked = currentSettings.googleAccount?.isNotBlank() == true

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
            }

            item {
                SettingItem(
                    "Automatic backup on Google drive",
                    currentSettings.backupFrequency.name.lowercase(),
                    onClick = {
                        if (!isGoogleLinked) {
                            showToast("Please sign in with Google to enable backups")
                        } else {
                            showIntervalDialog = true
                        }
                    },
                )
                if (isAuthorisingBackup) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if(isGoogleLinked){
                item {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formattedTime = String.format(
                            Locale.ENGLISH,
                            "%02d:%02d",
                            currentSettings.backupTimeHour,
                            currentSettings.backupTimeMinutes
                        )
                        SettingItem(
                            "Backup time",
                            formattedTime,
                            onClick = { showTimePickerDialog = true },
                            modifier = Modifier.weight(1f),
                            )

                        Button(
                            modifier = Modifier.padding(8.dp).clip(RectangleShape),
                            onClick = { onBackupNowClicked() }
                        ) {
                            Text("Backup Now")
                        }
                    }

                    BackupStatusUI(
                        backupUploadDownloadState = backupUploadDownloadState,
                        backupUiState = backupUiState,
                        onRestoreClicked = { onRestoreClicked() }
                    )
                }

                item {
                    Button(
                        modifier = Modifier.padding(8.dp).clip(RectangleShape).fillMaxWidth(),
                        onClick = { onDummyDataCreate() }
                    ) {
                        Text("Create dummy data")
                    }
                }

            }


            if(currentSettings.backupEnabled && currentSettings.googleAccount?.isNotBlank() == true) {
                item {
                    SettingItem(
                        "Google Account",
                        currentSettings.googleAccount,
                        onClick = {}
                    )
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

        if(showIntervalDialog){
            RadioButtonsAlertDialog(
                currentInterval= currentSettings.backupFrequency,
                entries = BackupFrequency.entries,
                onDismiss = {
                    showIntervalDialog = false
                },
                onConfirm = { backupFrequency ->
                    onBackupIntervalChanged(backupFrequency)
                    showIntervalDialog = false
                },
            )
        }

        if(showTimePickerDialog){
            TimePickerDialog(
                initialHour = currentSettings.backupTimeHour,
                initialMinute = currentSettings.backupTimeMinutes,
                onDismiss = { showTimePickerDialog = false }
            ){ hours, minutes ->
                onBackupTimeChanged(hours,minutes)
                showTimePickerDialog = false
            }
        }
    }
}


@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSettingsScreen(

) {
    NoteThePadTheme {
        SettingsScreen(
            onBackPressed = {},
            currentSettings = Settings(googleAccount="test@google.com"),
            isAuthorisingBackup = false,
            onThemeChanged = {},
            onBackupSettingsChanged = {},
            onBackupTimeChanged = {hours,minutes ->},
            onBackupIntervalChanged = {},
            showToast = {},
            onBackupNowClicked = {},
            backupUiState = BackupUiState.HasBackup(DriveFileMetadata("1", "Notes.db", 1708600000000L, 1024 * 1024 * 2)), // 2MB
            backupUploadDownloadState = BackupStatus.Idle,
            onLoadBackupInfo = {},
            onRestoreClicked = {},
            onDummyDataCreate = {}
        )
    }
}

