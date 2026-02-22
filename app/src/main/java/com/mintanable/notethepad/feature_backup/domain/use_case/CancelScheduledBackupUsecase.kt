package com.mintanable.notethepad.feature_backup.domain.use_case

import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency

class CancelScheduledBackupUsecase (
    private val backupScheduler: BackupScheduler
) {

    operator fun invoke() {
        backupScheduler.cancelBackup()
    }
}