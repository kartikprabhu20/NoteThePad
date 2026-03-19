package com.mintanable.notethepad.feature_ai.domain

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadCancelReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra("DOWNLOAD_ID", -1L)
        if (downloadId != -1L) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.remove(downloadId) // Stops the download and deletes the partial file

            WorkManager.getInstance(context).cancelAllWorkByTag(ModelDownloadWorker.MODEL_DOWNLOAD_TASK)

            scope.launch {
                userPreferencesRepository.updateAiModel("None")
                Log.d("kptest", "Download cancelled by user via notification, settings reset to None")
            }
        }
    }
}
