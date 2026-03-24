package com.mintanable.notethepad.feature_ai.domain.use_cases

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mintanable.notethepad.feature_ai.BuildConfig
import com.mintanable.notethepad.feature_ai.data.GeminiAudioModelDownloadWorker
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker.Companion.HF_TOKEN
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker.Companion.MODEL_FILE_NAME
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker.Companion.MODEL_URL
import javax.inject.Inject

class DownloadGeminiAudioTranscriberUseCase @Inject constructor(
    private val workManager: WorkManager,
) {

    operator fun invoke(modelUrl: String, fileName: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi only
            .setRequiresStorageNotLow(true)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<GeminiAudioModelDownloadWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName = GeminiAudioModelDownloadWorker.AUDIO_MODEL_DOWNLOAD_TASK ,
            ExistingWorkPolicy.KEEP,
            downloadRequest)
    }
}