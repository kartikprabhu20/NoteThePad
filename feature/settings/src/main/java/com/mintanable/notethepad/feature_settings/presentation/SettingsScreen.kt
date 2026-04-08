package com.mintanable.notethepad.feature_settings.presentation

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mintanable.notethepad.components.PermissionRationaleDialog
import com.mintanable.notethepad.components.TimePickerDialog
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import com.mintanable.notethepad.core.model.backup.DriveFileMetadata
import com.mintanable.notethepad.core.model.backup.LoadStatus
import com.mintanable.notethepad.core.model.settings.BackupFrequency
import com.mintanable.notethepad.core.model.settings.BackupSettings
import com.mintanable.notethepad.core.model.settings.Settings
import com.mintanable.notethepad.core.model.settings.ThemeMode
import com.mintanable.notethepad.feature_settings.R
import com.mintanable.notethepad.feature_settings.presentation.components.BackupStatusUI
import com.mintanable.notethepad.feature_settings.presentation.components.NoteShapePickerDialog
import com.mintanable.notethepad.feature_settings.presentation.components.RadioButtonsAlertDialog
import com.mintanable.notethepad.feature_settings.presentation.components.SettingItem
import com.mintanable.notethepad.feature_settings.presentation.components.SettingRadioGroup
import com.mintanable.notethepad.feature_settings.presentation.components.SettingSwitchItem
import com.mintanable.notethepad.feature_settings.presentation.components.noteShapeDisplayName
import com.mintanable.notethepad.permissions.PermissionRationaleType
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onBackPressed: () -> Unit,
    onNavigateToAiModelSelection: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    showToast: (String) -> Unit
) {

    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showIntervalDialog by rememberSaveable {  mutableStateOf(false) }
    var showTimePickerDialog by rememberSaveable { mutableStateOf(false) }
    var showClearBackupDialog by rememberSaveable { mutableStateOf(false) }
    var showNoteShapeDialog by rememberSaveable { mutableStateOf(false) }

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

                    Column {
                        BackupStatusUI(
                            backupUploadDownloadState = state.backupUploadDownloadState,
                            backupUiState = state.backupUiState,
                            onRestoreClicked = {
                                checkAndRequestNotificationPermission {
                                    onEvent(SettingsEvent.StartRestore(onFailure = showToast))
                                }
                            }
                        )

                        if (state.backupUiState is BackupUiState.HasBackup) {
                            Button(
                                onClick = { showClearBackupDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text(stringResource(R.string.btn_clear_backup))
                            }
                        }
                    }
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
                SettingSwitchItem(
                    title = stringResource(R.string.backup_media_title),
                    checked = currentSettings.backupSettings.backupMedia,
                    enableSettings = true,
                    onCheckedChange = { checked ->
                        onEvent(
                            SettingsEvent.UpdateBackupSettings(
                                backupSettings = currentSettings.backupSettings.copy(backupMedia = checked),
                                onAuthRequired = {},
                                onFailure = showToast
                            )
                        )
                    }
                )
            }

            item {
                SettingSwitchItem(
                    title = stringResource(R.string.supasync_title),
                    subtitle = stringResource(R.string.supasync_description),
                    checked = currentSettings.supaSyncEnabled,
                    enableSettings = true,
                    onCheckedChange = { checked ->
                        onEvent(SettingsEvent.UpdateSupaSync(checked))
                    }
                )
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
                    onClick = onNavigateToAiModelSelection
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

                val audioTranscriberSubtitle = when (state.audioTranscriberStatus) {
                    AiModelDownloadStatus.Ready -> stringResource(R.string.audio_transcriber_status_ready)
                    AiModelDownloadStatus.Downloading -> stringResource(R.string.audio_transcriber_status_downloading)
                    AiModelDownloadStatus.Downloadable -> stringResource(R.string.audio_transcriber_status_downloadable)
                    AiModelDownloadStatus.Unavailable -> stringResource(R.string.audio_transcriber_status_unavailable)
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
                SettingItem(
                    title = stringResource(R.string.setting_note_shape),
                    subtitle = noteShapeDisplayName(currentSettings.noteShape),
                    onClick = { showNoteShapeDialog = true }
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

        if (state.showDownloadAudioTranscriberDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(SettingsEvent.DismissDownloadAudioTranscriberDialog) },
                title = { Text(stringResource(R.string.dialog_download_audio_transcriber_title)) },
                text = { Text(stringResource(R.string.dialog_download_audio_transcriber_description)) },
                confirmButton = {
                    TextButton(onClick = { onEvent(SettingsEvent.ConfirmDownloadAudioTranscriber) }) {
                        Text(stringResource(R.string.btn_download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(SettingsEvent.DismissDownloadAudioTranscriberDialog) }) {
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

        if (showNoteShapeDialog) {
            NoteShapePickerDialog(
                currentShape = currentSettings.noteShape,
                onShapeSelected = { shape -> onEvent(SettingsEvent.UpdateNoteShape(shape)) },
                onDismiss = { showNoteShapeDialog = false },
                isDarkTheme = if (state.settings.themeMode == ThemeMode.SYSTEM) isSystemInDarkTheme() else state.settings.themeMode == ThemeMode.DARK
            )
        }

        if (showClearBackupDialog) {
            AlertDialog(
                onDismissRequest = { showClearBackupDialog = false },
                title = { Text(stringResource(R.string.dialog_clear_backup_title)) },
                text = { Text(stringResource(R.string.dialog_clear_backup_description)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearBackupDialog = false
                            onEvent(SettingsEvent.ClearAppData(onFailure = showToast))
                        }
                    ) {
                        Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearBackupDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@ThemePreviews
@Composable
fun PreviewSettingsScreen() {
    NoteThePadTheme {
        SettingsScreen(
            state = SettingsState(
                settings = Settings(googleAccount = "test@google.com", backupSettings = BackupSettings(backupFrequency= BackupFrequency.DAILY)),
                backupUiState = BackupUiState.HasBackup(
                    DriveFileMetadata(
                        "1",
                        "Notes.db",
                        1708600000000L,
                        1024 * 1024 * 2
                    )
                )
            ),
            onBackPressed = {},
            onNavigateToAiModelSelection = {},
            onEvent = {},
            showToast = {}
        )
    }
}
