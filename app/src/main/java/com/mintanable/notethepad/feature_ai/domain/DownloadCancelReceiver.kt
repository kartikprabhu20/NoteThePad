package com.mintanable.notethepad.feature_ai.domain

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra("DOWNLOAD_ID", -1L)
        if (downloadId != -1L) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.remove(downloadId) // Stops the download and deletes the partial file

            WorkManager.getInstance(context).cancelAllWorkByTag(ModelDownloadWorker.MODEL_DOWNLOAD_TASK)

            Log.d("kptest", "Download cancelled by user via notification")
        }
    }
}
