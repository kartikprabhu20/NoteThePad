package com.mintanable.notethepad.feature_ai.domain.use_case

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker.Companion.MODEL_FILE_NAME
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker.Companion.MODEL_URL
import com.mintanable.notethepad.feature_ai.domain.model.AiModelType
import javax.inject.Inject

class DownloadAiModelUseCase @Inject constructor(
    private val workManager: WorkManager,
) {

    operator fun invoke(
        type: AiModelType
    ) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi only
            .setRequiresStorageNotLow(true)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    MODEL_URL to type.modelUrl,
                    MODEL_FILE_NAME to type.fileName
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName = ModelDownloadWorker.MODEL_DOWNLOAD_TASK ,
            ExistingWorkPolicy.KEEP,
            downloadRequest)
    }
}