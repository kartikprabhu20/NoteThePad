package com.mintanable.notethepad.feature_backup.domain.use_case

import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import com.mintanable.notethepad.feature_settings.domain.model.BackupSettings
import javax.inject.Inject

class ScheduleBackupUseCase @Inject constructor(
    private val backupScheduler: BackupScheduler
) {

    operator fun invoke(
        backupSettings: BackupSettings,
        backupNow: Boolean
    ) {
        backupScheduler.scheduleBackup(
            backupSettings,
            backupNow
        )
    }
}