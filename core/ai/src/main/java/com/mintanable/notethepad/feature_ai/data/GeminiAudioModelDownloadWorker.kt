package com.mintanable.notethepad.feature_ai.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.mintanable.notethepad.core.model.NoteThePadConstants.DOWNLOAD_MODEL_NOTIFICATION_CHANNEL_ID
import com.mintanable.notethepad.core.model.NoteThePadConstants.DOWNLOAD_MODEL_NOTIFICATION_ID
import com.mintanable.notethepad.feature_ai.R
import com.mintanable.notethepad.feature_ai.domain.DownloadCancelReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@HiltWorker
class GeminiAudioModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val AUDIO_MODEL_DOWNLOAD_TASK = "audio_model_download_task"
        const val DOWNLOAD_ID = 1003L
        const val DOWNLOAD_ID_EXTRA = "DOWNLOAD_ID_EXTRA"
    }

    private val options = SpeechRecognizerOptions.builder().apply {
        locale = Locale.US
        preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
    }.build()

    @SuppressLint("Range", "RestrictedApi")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val speechRecognizer = SpeechRecognition.getClient(options)
        var totalBytesToDownload = 0L
        var result: Result = Result.success()

        try {
            speechRecognizer.download().collect { downloadStatus ->
                when (downloadStatus) {
                    is DownloadStatus.DownloadStarted -> {
                        totalBytesToDownload = downloadStatus.bytesToDownload
                    }

                    is DownloadStatus.DownloadProgress -> {
                        val progress = if (totalBytesToDownload > 0) {
                            100 * downloadStatus.totalBytesDownloaded / totalBytesToDownload
                        } else 0
                        setProgress(workDataOf("percent" to progress))
                        setForeground(
                            createForegroundInfo(
                                downloadId = DOWNLOAD_ID,
                                progress.toInt(),
                                "Gemini Nano Audio Model"
                            )
                        )
                        Log.d("kptest", "Downloading audio model: $progress%")
                    }

                    is DownloadStatus.DownloadCompleted -> {
                        setProgress(workDataOf("percent" to 100))
                        Log.d("kptest", "Download complete. You can now call startRecognition().")
                    }

                    is DownloadStatus.DownloadFailed -> {
                        val error = downloadStatus.e
                        Log.d("kptest", "Audio model Download failed: ${error.message}")
                        result = Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiAudioModelWorker", "Error during download", e)
            result = Result.failure()
        } finally {
            speechRecognizer.close()
        }
        return@withContext result
    }


    private fun createForegroundInfo(
        downloadId: Long,
        progress: Int,
        modelName: String
    ): ForegroundInfo {
        var pendingCancelIntent: PendingIntent? = null

        if (downloadId > -1) {
            val cancelIntent =
                Intent(applicationContext, DownloadCancelReceiver::class.java).apply {
                    putExtra(DOWNLOAD_ID_EXTRA, downloadId)
                }

            pendingCancelIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification =
            NotificationCompat.Builder(applicationContext, DOWNLOAD_MODEL_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Audio AI Model Sync")
                .setContentText("Downloading $modelName: $progress%")
                .setSmallIcon(R.drawable.notethepad_launcher_foreground)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .addAction(R.drawable.baseline_cancel_24, "Cancel", pendingCancelIntent)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                DOWNLOAD_MODEL_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(DOWNLOAD_MODEL_NOTIFICATION_ID, notification)
        }
    }
}