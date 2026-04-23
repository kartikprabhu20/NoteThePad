package com.mintanable.notethepad.feature_settings.presentation

import android.app.PendingIntent
import android.content.Intent
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.settings.BackupSettings
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.core.model.settings.ThemeMode

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
    data object AuthCancelled : SettingsEvent()
    data class StartRestore(val onFailure: (String) -> Unit) : SettingsEvent()
    data class SelectAiModel(val aiModel: AiModel) : SettingsEvent()
    data class ConfirmDownloadAiModel(
        val aiModel: AiModel,
        val onFailure: (String) -> Unit
    ) : SettingsEvent()
    data object DismissDownloadDialog : SettingsEvent()
    data object SignOut : SettingsEvent()
    data class ClearAppData(val onFailure: (String) -> Unit) : SettingsEvent()
    data class DeleteAllCloudData(
        val onSuccess: () -> Unit,
        val onFailure: (String) -> Unit
    ) : SettingsEvent()
    data object RequestDownloadAudioTranscriber : SettingsEvent()
    data object ConfirmDownloadAudioTranscriber : SettingsEvent()
    data object DismissDownloadAudioTranscriberDialog : SettingsEvent()
    data class UpdateNoteShape(val noteShape: NoteShape) : SettingsEvent()
    data class UpdateSupaSync(val enabled: Boolean) : SettingsEvent()
    data class DeleteAiModel(val aiModel: AiModel) : SettingsEvent()
    data object CompleteOnboarding : SettingsEvent()
    data class UpdateAnalyticsEnabled(val enabled: Boolean) : SettingsEvent()
    data class EnableAIAssistant(val enabled: Boolean) : SettingsEvent()
}
