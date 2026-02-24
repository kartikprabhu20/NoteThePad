package com.mintanable.notethepad.feature_settings.presentation

import android.util.Log
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_backup.presentation.UploadDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

fun WorkManager.getBackupStatusFlow(
    workName: String,
    type: UploadDownload,
    onSuccess: () -> Unit = {}
): Flow<BackupStatus> {
    return this.getWorkInfosForUniqueWorkFlow(workName)
        .map { list ->
            val workInfo = list.firstOrNull() ?: return@map BackupStatus.Idle
            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt("percent", 0)
                    Log.d("kptest", "[${type.name}] status progress:$progress")
                    BackupStatus.Progress(progress, type)
                }
                WorkInfo.State.SUCCEEDED -> {
                    Log.d("kptest", "[${type.name}] successful")
                    onSuccess()
                    BackupStatus.Success
                }
                WorkInfo.State.FAILED -> BackupStatus.Error("Background ${type.name.lowercase()} failed")
                else -> BackupStatus.Idle
            }
        }
        .distinctUntilChanged()
}