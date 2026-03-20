package com.mintanable.notethepad.feature_backup.domain

import androidx.work.Data
import com.mintanable.notethepad.core.model.settings.BackupSettings

interface BackupScheduler {
    fun scheduleBackup(backupSettings: BackupSettings, backupNow: Boolean = false)
    fun cancelBackup()
    fun onWorkCompleted(inputData: Data)
}