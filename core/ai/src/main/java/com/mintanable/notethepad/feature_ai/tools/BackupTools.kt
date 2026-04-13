package com.mintanable.notethepad.feature_ai.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.mintanable.notethepad.core.model.settings.BackupFrequency
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.use_case.CancelScheduledBackupUsecase
import com.mintanable.notethepad.feature_backup.domain.use_case.DownloadBackup
import com.mintanable.notethepad.feature_backup.domain.use_case.ScheduleBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupTools @Inject constructor(
    private val scheduleBackupUseCase: ScheduleBackupUseCase,
    private val cancelScheduledBackupUsecase: CancelScheduledBackupUsecase,
    private val downloadBackup: DownloadBackup,
    private val googleAuthRepository: GoogleAuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ToolSet {

    @Tool(description = "Schedules Google Drive backup at the given frequency (OFF, DAILY, WEEKLY, MONTHLY). Returns 'ok' or 'invalid_frequency'.")
    fun scheduleBackup(
        @ToolParam(description = "Frequency: OFF, DAILY, WEEKLY, or MONTHLY") frequency: String,
    ): String = runBlocking(Dispatchers.IO) {
        val parsed = runCatching {
            BackupFrequency.valueOf(frequency.trim().uppercase())
        }.getOrNull() ?: return@runBlocking "invalid_frequency"
        val currentSettings = userPreferencesRepository.settingsFlow.first().backupSettings
        scheduleBackupUseCase(
            backupSettings = currentSettings.copy(backupFrequency = parsed),
            backupNow = false,
        )
        "ok"
    }

    @Tool(description = "Cancels any scheduled Google Drive backup. Returns 'ok'.")
    fun cancelScheduledBackup(): String = runBlocking(Dispatchers.IO) {
        cancelScheduledBackupUsecase()
        "ok"
    }

    @Tool(description = "Triggers an immediate Google Drive backup using current settings. Returns 'ok'.")
    fun triggerBackupNow(): String = runBlocking(Dispatchers.IO) {
        val currentSettings = userPreferencesRepository.settingsFlow.first().backupSettings
        scheduleBackupUseCase(backupSettings = currentSettings, backupNow = true)
        "ok"
    }

    @Tool(description = "Downloads (restores) the latest backup from Google Drive. Returns 'ok'.")
    fun restoreFromBackup(): String {
        downloadBackup()
        return "ok"
    }

    @Tool(description = "Returns 'true' if the user has authorized Drive access, otherwise 'false'.")
    fun isUserSignedInForBackup(): String = runBlocking(Dispatchers.IO) {
        googleAuthRepository.hasDriveAccess().toString()
    }
}
