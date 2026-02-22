package com.mintanable.notethepad.feature_backup.domain.use_case

import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import javax.inject.Inject

class CancelScheduledBackupUsecase @Inject constructor(
    private val backupScheduler: BackupScheduler
) {

    operator fun invoke() {
        backupScheduler.cancelBackup()
    }
}