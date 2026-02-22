package com.mintanable.notethepad.feature_backup.presentation

sealed class BackupStatus {
    object Idle : BackupStatus()
    data class Progress(val percentage: Int) : BackupStatus()
    object Success : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}
