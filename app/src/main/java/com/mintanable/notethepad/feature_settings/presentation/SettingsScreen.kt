package com.mintanable.notethepad.feature_settings.presentation

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_backup.presentation.BackupUiState
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import com.mintanable.notethepad.feature_settings.domain.model.BackupSettings
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.components.BackupStatusUI
import com.mintanable.notethepad.feature_settings.presentation.components.PermissionRationaleDialog
import com.mintanable.notethepad.feature_settings.presentation.components.RadioButtonsAlertDialog
import com.mintanable.notethepad.feature_settings.presentation.components.SettingItem
import com.mintanable.notethepad.feature_settings.presentation.components.SettingRadioGroup
import com.mintanable.notethepad.feature_settings.presentation.components.TimePickerDialog
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    currentSettings: Settings,
    onLoadBackupInfo: () -> Unit,
    backupUploadDownloadState: BackupStatus,
    backupUiState: BackupUiState,
    isAuthorisingBackup: Boolean,
    onThemeChanged: (ThemeMode) -> Unit,
    showToast: (String) -> Unit,
    onBackupNowClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onDummyDataCreate: () -> Unit,
    onBackupSettingsChanged: (BackupSettings) -> Unit
) {

    LaunchedEffect(Unit) {
        onLoadBackupInfo()
    }

    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showIntervalDialog by rememberSaveable {  mutableStateOf(false) }
    var showTimePickerDialog by rememberSaveable { mutableStateOf(false) }
    val isGoogleLinked = currentSettings.googleAccount?.isNotBlank() == true
    val notificationPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS
    )
    val checkAndRequestNotificationPermission = { action: () -> Unit ->
        action()

        when{
            notificationPermissionState.status.isGranted -> { } //Do nothing
            notificationPermissionState.status.shouldShowRationale -> { showRationaleDialog = true }
            else -> { notificationPermissionState.launchPermissionRequest() }
        }
    }

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
                SettingItem(
                    "Automatic backup on Google drive",
                    currentSettings.backupSettings.backupFrequency.name.lowercase(),
                    onClick = {
                        if (!isGoogleLinked) {
                            showToast("Please sign in with Google to enable backups")
                        } else {
                            showIntervalDialog = true
                        }
                    },
                )
                if (isAuthorisingBackup) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }
            }

            if(isGoogleLinked && currentSettings.backupSettings.backupFrequency != BackupFrequency.OFF){
                item {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formattedTime = String.format(
                            Locale.ENGLISH,
                            "%02d:%02d",
                            currentSettings.backupSettings.backupTimeHour,
                            currentSettings.backupSettings.backupTimeMinutes
                        )
                        SettingItem(
                            "Backup time",
                            formattedTime,
                            onClick = { showTimePickerDialog = true },
                            modifier = Modifier.weight(1f),
                            )

                        Button(
                            modifier = Modifier.padding(8.dp).clip(RectangleShape),
                            onClick = { checkAndRequestNotificationPermission{onBackupNowClicked()} }
                        ) {
                            Text("Backup Now")
                        }
                    }

                    BackupStatusUI(
                        backupUploadDownloadState = backupUploadDownloadState,
                        backupUiState = backupUiState,
                        onRestoreClicked = { checkAndRequestNotificationPermission { onRestoreClicked() }}
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
                currentInterval= currentSettings.backupSettings.backupFrequency,
                entries = BackupFrequency.entries,
                onDismiss = {
                    showIntervalDialog = false
                },
                onConfirm = { backupFrequency ->
                    val newSettings = currentSettings.backupSettings.copy(backupFrequency = backupFrequency)
                    if (backupFrequency != BackupFrequency.OFF) {
                        checkAndRequestNotificationPermission { onBackupSettingsChanged(newSettings) }
                    } else {
                        onBackupSettingsChanged(newSettings)
                    }
                    showIntervalDialog = false
                },
            )
        }

        if(showTimePickerDialog){
            TimePickerDialog(
                initialHour = currentSettings.backupSettings.backupTimeHour,
                initialMinute = currentSettings.backupSettings.backupTimeMinutes,
                onDismiss = { showTimePickerDialog = false }
            ){ hours, minutes ->
                onBackupSettingsChanged(currentSettings.backupSettings.copy(backupTimeHour = hours, backupTimeMinutes = minutes))
                showTimePickerDialog = false
            }
        }

        if (showRationaleDialog) {
            PermissionRationaleDialog(
                onConfirmClicked = {
                    showRationaleDialog = false
                    notificationPermissionState.launchPermissionRequest()
                },
                onDismissRequest = {showRationaleDialog = false}
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
            onLoadBackupInfo = {},
            backupUploadDownloadState = BackupStatus.Idle,
            backupUiState = BackupUiState.HasBackup(DriveFileMetadata("1", "Notes.db", 1708600000000L, 1024 * 1024 * 2)),
            isAuthorisingBackup = false,
            onThemeChanged = {},
            onBackupSettingsChanged = {},
            showToast = {},
            onBackupNowClicked = {},
            onRestoreClicked = {},
            onDummyDataCreate = {},
        )
    }
}

