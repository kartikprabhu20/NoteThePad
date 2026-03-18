package com.mintanable.notethepad.feature_settings.presentation

import com.mintanable.notethepad.core.model.AiModel
import com.mintanable.notethepad.core.model.LoadStatus
import com.mintanable.notethepad.core.model.Settings

data class SettingsState(
    val settings: Settings = Settings(),
    val backupUploadDownloadState: LoadStatus = LoadStatus.Idle,
    val aiModels: List<AiModel> = emptyList(),
    val aiModelDownloadStatus: LoadStatus = LoadStatus.Idle,
    val backupUiState: BackupUiState = BackupUiState.Loading,
    val isAuthorisingBackup: Boolean = false,
    val showDownloadModelDialog: AiModel? = null
)