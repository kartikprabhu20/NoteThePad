package com.mintanable.notethepad.feature_settings.presentation

import com.mintanable.notethepad.core.model.DriveFileMetadata

sealed class BackupUiState {
    object Loading : BackupUiState()
    data class HasBackup(val metadata: DriveFileMetadata) : BackupUiState()
    object NoBackup : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}