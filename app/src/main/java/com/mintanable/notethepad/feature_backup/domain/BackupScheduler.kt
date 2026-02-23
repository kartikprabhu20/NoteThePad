package com.mintanable.notethepad.feature_backup.domain

import androidx.work.Data
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import com.mintanable.notethepad.feature_settings.domain.model.BackupSettings

interface BackupScheduler {
    fun scheduleBackup(backupSettings: BackupSettings, backupNow: Boolean = false)
    fun cancelBackup()
    fun onWorkCompleted(inputData: Data)
}