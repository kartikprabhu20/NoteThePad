package com.mintanable.notethepad.feature_settings.presentation

import android.util.Log
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.mintanable.notethepad.feature_backup.presentation.LoadStatus
import com.mintanable.notethepad.feature_backup.presentation.LoadType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

fun WorkManager.getLoadStatusFLow(
    workName: String,
    type: LoadType,
    onSuccess: () -> Unit = {}
): Flow<LoadStatus> {
    return this.getWorkInfosForUniqueWorkFlow(workName)
        .map { list ->
            val workInfo = list.firstOrNull() ?: return@map LoadStatus.Idle
            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt("percent", 0)
                    Log.d("kptest", "[${type.name}] status progress:$progress")
                    LoadStatus.Progress(progress, type)
                }
                WorkInfo.State.SUCCEEDED -> {
                    Log.d("kptest", "[${type.name}] successful")
                    onSuccess()
                    LoadStatus.Success
                }
                WorkInfo.State.FAILED -> {
                    Log.d("kptest", "[${type.name}] failed")
                    LoadStatus.Error("Background ${type.name.lowercase()} failed")
                }
                else -> LoadStatus.Idle
            }
        }
        .distinctUntilChanged()
}