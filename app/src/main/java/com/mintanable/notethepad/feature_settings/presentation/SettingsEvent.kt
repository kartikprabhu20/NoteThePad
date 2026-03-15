package com.mintanable.notethepad.feature_settings.presentation

import android.app.PendingIntent
import android.content.Intent
import com.mintanable.notethepad.feature_ai.domain.model.AiModel
import com.mintanable.notethepad.feature_settings.domain.model.BackupSettings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode

sealed class SettingsEvent {
    data class UpdateTheme(val themeMode: ThemeMode) : SettingsEvent()
    data class UpdateBackupSettings(
        val backupSettings: BackupSettings,
        val backupNow: Boolean = false,
        val onAuthRequired: (PendingIntent) -> Unit,
        val onFailure: (String) -> Unit
    ) : SettingsEvent()
    data class AuthResultCompleted(
        val intent: Intent?,
        val onFailure: (String) -> Unit
    ) : SettingsEvent()
    object AuthCancelled : SettingsEvent()
    data class StartRestore(val onFailure: (String) -> Unit) : SettingsEvent()
    object CreateDummyData : SettingsEvent()
    data class ChangeAiModel(val modelName: String) : SettingsEvent()
    data class DownloadAiModel(val aiModel: AiModel) : SettingsEvent()
    object SignOut : SettingsEvent()
}
