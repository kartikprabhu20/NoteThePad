package com.mintanable.notethepad.feature_backup.presentation

sealed class BackupUiState {
    object Loading : BackupUiState()
    data class HasBackup(val metadata: DriveFileMetadata) : BackupUiState()
    object NoBackup : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}