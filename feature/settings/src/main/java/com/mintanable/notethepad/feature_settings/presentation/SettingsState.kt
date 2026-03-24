package com.mintanable.notethepad.feature_settings.presentation

import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import com.mintanable.notethepad.core.model.backup.LoadStatus
import com.mintanable.notethepad.core.model.settings.Settings

data class SettingsState(
    val settings: Settings = Settings(),
    val backupUploadDownloadState: LoadStatus = LoadStatus.Idle,
    val aiModels: List<AiModel> = emptyList(),
    val aiModelDownloadStatus: LoadStatus = LoadStatus.Idle,
    val backupUiState: BackupUiState = BackupUiState.Loading,
    val isAuthorisingBackup: Boolean = false,
    val showDownloadModelDialog: AiModel? = null,
    val audioTranscriberStatus: AiModelDownloadStatus = AiModelDownloadStatus.Unavailable,
    val showDownloadAudioTranscriberDialog: Boolean = false,
    val audioModelStatus: LoadStatus = LoadStatus.Idle
)