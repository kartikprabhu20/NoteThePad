package com.mintanable.notethepad.core.worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_backup.presentation.BackupWorker
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import java.util.concurrent.TimeUnit
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

const val KEY_FREQUENCY = "frequency"
const val KEY_HOUR = "hour"
const val KEY_MINUTE = "minute"

class BackupSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupScheduler {
    companion object {
        const val WORK_NAME = "backup_work"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun scheduleBackup(frequency: BackupFrequency, hour: Int, minute: Int) {
        val interval = when (frequency) {
            BackupFrequency.DAILY -> 1L to TimeUnit.DAYS
            BackupFrequency.WEEKLY -> 7L to TimeUnit.DAYS
            BackupFrequency.MONTHLY -> 30L to TimeUnit.DAYS
            BackupFrequency.OFF -> return
        }

        if(frequency==BackupFrequency.MONTHLY)
            scheduleMonthly(hour,minute)
        else
            schedulePeriodic(interval,frequency, hour, minute)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleMonthly(hour: Int, minute: Int) {

        val delay = ScheduleHelper.calculateNextMonthlyDelay(hour, minute)

        val request =
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        KEY_FREQUENCY to BackupFrequency.MONTHLY.name,
                        KEY_HOUR to hour,
                        KEY_MINUTE to minute
                    )
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun schedulePeriodic(interval: Pair<Long, TimeUnit>, frequency: BackupFrequency, hour: Int, minute: Int){
        val delay = ScheduleHelper.calculateInitialDelay(hour, minute, frequency)

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            interval.first,
            interval.second
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onWorkCompleted(inputData: Data) {
        val frequency = inputData.getString("frequency")
            ?.let { BackupFrequency.valueOf(it) }
            ?: return

        if (frequency == BackupFrequency.MONTHLY) {
            val hour = inputData.getInt(KEY_HOUR, 2)
            val minute = inputData.getInt(KEY_MINUTE, 0)
             scheduleMonthly(hour, minute)
        }
    }

}