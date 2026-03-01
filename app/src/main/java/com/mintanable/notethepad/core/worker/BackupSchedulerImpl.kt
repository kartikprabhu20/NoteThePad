package com.mintanable.notethepad.core.worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_backup.presentation.BackupWorker
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import java.util.concurrent.TimeUnit
import androidx.work.workDataOf
import com.mintanable.notethepad.feature_settings.domain.model.BackupSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BackupSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupScheduler {

    companion object {
        const val WORK_NAME = "backup_work"
        const val KEY_FREQUENCY = "frequency"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_BACKUP_NOW = "backupNow"
    }

    override fun scheduleBackup(backupSettings: BackupSettings, backupNow: Boolean) {
        val frequency = backupSettings.backupFrequency
        val hour = backupSettings.backupTimeHour
        val minute = backupSettings.backupTimeMinutes

        val workdata = workDataOf(
            KEY_FREQUENCY to frequency.name,
            KEY_HOUR to hour,
            KEY_MINUTE to minute,
            KEY_BACKUP_NOW to backupNow
        )

        if (backupNow) {
            scheduleImmediately(workdata)
            return
        }

        if (frequency == BackupFrequency.OFF) {
            cancelBackup()
            return
        }

        if(frequency==BackupFrequency.MONTHLY) {
            scheduleMonthly(hour, minute, workdata)
        } else {
            val interval = when (frequency) {
                BackupFrequency.DAILY -> 1L to TimeUnit.DAYS
                BackupFrequency.WEEKLY -> 7L to TimeUnit.DAYS
                else -> 1L to TimeUnit.DAYS
            }
            schedulePeriodic(interval, frequency, hour, minute, workdata)
        }
    }

    private fun scheduleImmediately(workdata: Data){
        val instantRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // Priority
            .setInputData(workdata)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            instantRequest
        )

    }

    private fun scheduleMonthly(hour: Int, minute: Int, workdata: Data) {

        val delay = ScheduleHelper.calculateNextMonthlyDelay(hour, minute)

        val request =
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workdata)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    private fun schedulePeriodic(interval: Pair<Long, TimeUnit>, frequency: BackupFrequency, hour: Int, minute: Int, workdata: Data){
        val delay = ScheduleHelper.calculateInitialDelay(hour, minute, frequency)

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            interval.first,
            interval.second
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workdata)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    override fun cancelBackup() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    override fun onWorkCompleted(inputData: Data) {
        val frequency = inputData.getString(KEY_FREQUENCY)
            ?.let { BackupFrequency.valueOf(it) }
            ?: return

        val backupNow = inputData.getBoolean(KEY_BACKUP_NOW, false)

        if(backupNow || frequency == BackupFrequency.MONTHLY){
            val hour = inputData.getInt(KEY_HOUR, 2)
            val minute = inputData.getInt(KEY_MINUTE, 0)
            scheduleBackup(BackupSettings(frequency, hour, minute), false)
        }
    }
}