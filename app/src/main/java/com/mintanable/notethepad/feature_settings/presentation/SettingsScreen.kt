package com.mintanable.notethepad.feature_settings.presentation

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_ai.domain.model.AiModel
import com.mintanable.notethepad.feature_ai.presentation.humanReadableSize
import com.mintanable.notethepad.feature_backup.presentation.LoadStatus
import com.mintanable.notethepad.feature_backup.presentation.BackupUiState
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.components.AiModelSelectionDialog
import com.mintanable.notethepad.feature_settings.presentation.components.BackupStatusUI
import com.mintanable.notethepad.feature_settings.presentation.components.PermissionRationaleDialog
import com.mintanable.notethepad.feature_settings.presentation.components.RadioButtonsAlertDialog
import com.mintanable.notethepad.feature_settings.presentation.components.SettingItem
import com.mintanable.notethepad.feature_settings.presentation.components.SettingRadioGroup
import com.mintanable.notethepad.feature_settings.presentation.components.TimePickerDialog
import com.mintanable.notethepad.feature_settings.presentation.util.PermissionRationaleType
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onBackPressed: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    showToast: (String) -> Unit
) {

    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showIntervalDialog by rememberSaveable {  mutableStateOf(false) }
    var showTimePickerDialog by rememberSaveable { mutableStateOf(false) }
    var showAiModelDialog by rememberSaveable { mutableStateOf(false) }
    var modelToDownload by remember { mutableStateOf<AiModel?>(null) }

    val currentSettings = state.settings
    val isGoogleLinked = currentSettings.googleAccount?.isNotBlank() == true

    val notificationPermissionState = if (LocalInspectionMode.current) {
        remember {
            object : PermissionState {
                override val permission: String = Manifest.permission.POST_NOTIFICATIONS
                override val status: PermissionStatus = PermissionStatus.Granted
                override fun launchPermissionRequest() {}
            }
        }
    } else {
        rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    val msgSigninGoogleBackups = stringResource(R.string.msg_signin_google_backups)
    val checkAndRequestNotificationPermission = { action: () -> Unit ->
        action() //Perform action in anycase, since backup progress can be foreground too

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
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                        },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
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
                Text(
                    text = stringResource(R.string.setting_backup),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                SettingItem(
                    stringResource(R.string.setting_auto_backup_title),
                    currentSettings.backupSettings.backupFrequency.name.lowercase(),
                    onClick = {
                        if (!isGoogleLinked) {
                            showToast(msgSigninGoogleBackups)
                        } else {
                            showIntervalDialog = true
                        }
                    },
                )
                if (state.isAuthorisingBackup) {
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
                            stringResource(R.string.setting_backup_time),
                            formattedTime,
                            onClick = { showTimePickerDialog = true },
                            modifier = Modifier.weight(1f),
                            )

                        Button(
                            modifier = Modifier.padding(8.dp).clip(RectangleShape),
                            onClick = { checkAndRequestNotificationPermission {
                                onEvent(SettingsEvent.UpdateBackupSettings(
                                    backupSettings = currentSettings.backupSettings,
                                    backupNow = true,
                                    onAuthRequired = {}, // Handled in MainActivity
                                    onFailure = showToast
                                ))
                            } }
                        ) {
                            Text(stringResource(R.string.btn_backup_now))
                        }
                    }

                    BackupStatusUI(
                        backupUploadDownloadState = state.backupUploadDownloadState,
                        backupUiState = state.backupUiState,
                        onRestoreClicked = { checkAndRequestNotificationPermission {
                            onEvent(SettingsEvent.StartRestore(onFailure = showToast))
                        }}
                    )
                }
            }


            if(currentSettings.backupEnabled && currentSettings.googleAccount?.isNotBlank() == true) {
                item {
                    SettingItem(
                        stringResource(R.string.setting_google_account),
                        currentSettings.googleAccount ?: "",
                        onClick = {}
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.setting_ai_assistant),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                SettingItem(
                    title = stringResource(R.string.setting_ai_model),
                    subtitle = state.aiModels.find { it.name == currentSettings.aiModelName }?.displayName
                        ?: stringResource(R.string.loading),
                    onClick = { showAiModelDialog = true }
                )
                if (state.aiModelDownloadStatus is LoadStatus.Progress) {
                    Text(stringResource(R.string.msg_downloading_model, state.aiModelDownloadStatus.percentage),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    LinearProgressIndicator(
                        progress = { state.aiModelDownloadStatus.percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.setting_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                SettingRadioGroup(
                    title = stringResource(R.string.setting_theme_modes),
                    selectedOption = currentSettings.themeMode,
                    onOptionSelected = { onEvent(SettingsEvent.UpdateTheme(it)) },
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
                        checkAndRequestNotificationPermission {
                            onEvent(SettingsEvent.UpdateBackupSettings(
                                backupSettings = newSettings,
                                onAuthRequired = {},
                                onFailure = showToast
                            ))
                        }
                    } else {
                        onEvent(SettingsEvent.UpdateBackupSettings(
                            backupSettings = newSettings,
                            onAuthRequired = {},
                            onFailure = showToast
                        ))
                    }
                    showIntervalDialog = false
                },
            )
        }

        if(showAiModelDialog) {
            AiModelSelectionDialog(
                currentModel = currentSettings.aiModelName,
                aiModels = state.aiModels,
                onDismiss = { showAiModelDialog = false },
                onConfirm = { selectedModelName ->
                    val aiModel = state.aiModels.find { it.name == selectedModelName }
                    aiModel?.let {
                        if (aiModel.url.isNotEmpty()) {
                            modelToDownload = aiModel
                        } else {
                            onEvent(SettingsEvent.ChangeAiModel(selectedModelName))
                        }
                    }
                    showAiModelDialog = false
                }
            )
        }

        modelToDownload?.let { model ->
            AlertDialog(
                onDismissRequest = { modelToDownload = null },
                title = { Text(stringResource(R.string.dialog_download_ai_model_title)) },
                text = { Text(stringResource(R.string.dialog_download_ai_model_description, model.sizeInBytes.humanReadableSize())) },
                confirmButton = {
                    TextButton(onClick = {
                        onEvent(SettingsEvent.ChangeAiModel(model.name))
                        onEvent(SettingsEvent.DownloadAiModel(model))
                        modelToDownload = null
                    }) {
                        Text(stringResource(R.string.btn_download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { modelToDownload = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        if(showTimePickerDialog){
            TimePickerDialog(
                initialHour = currentSettings.backupSettings.backupTimeHour,
                initialMinute = currentSettings.backupSettings.backupTimeMinutes,
                onDismiss = { showTimePickerDialog = false }
            ){ hours, minutes ->
                onEvent(SettingsEvent.UpdateBackupSettings(
                    backupSettings = currentSettings.backupSettings.copy(backupTimeHour = hours, backupTimeMinutes = minutes),
                    onAuthRequired = {},
                    onFailure = showToast
                ))
                showTimePickerDialog = false
            }
        }

        if (showRationaleDialog) {
            PermissionRationaleDialog(
                permissionRationaleType = PermissionRationaleType.NOTIFICATION,
                onConfirmClicked = {
                    showRationaleDialog = false
                    notificationPermissionState.launchPermissionRequest()
                },
                onDismissRequest = { showRationaleDialog = false }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSettingsScreen() {
    NoteThePadTheme {
        SettingsScreen(
            state = SettingsState(
                settings = Settings(googleAccount="test@google.com"),
                backupUiState = BackupUiState.HasBackup(DriveFileMetadata("1", "Notes.db", 1708600000000L, 1024 * 1024 * 2))
            ),
            onBackPressed = {},
            onEvent = {},
            showToast = {}
        )
    }
}
