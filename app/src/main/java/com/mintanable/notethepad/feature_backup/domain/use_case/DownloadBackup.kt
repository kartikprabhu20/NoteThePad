package com.mintanable.notethepad.feature_backup.domain.use_case

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.mintanable.notethepad.feature_backup.presentation.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DownloadBackup @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    companion object {
        const val DOWNLOAD_BACKUP_TASK = "download_backup_task"
    }

    operator fun invoke() {
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // Priority
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("download_backup_work")
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            DOWNLOAD_BACKUP_TASK,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}