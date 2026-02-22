package com.mintanable.notethepad.feature_backup.domain

import androidx.work.Data
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency

interface BackupScheduler {
    fun scheduleBackup(frequency: BackupFrequency, hour: Int, minute: Int)
    fun cancelBackup()
    fun onWorkCompleted(inputData: Data)
}