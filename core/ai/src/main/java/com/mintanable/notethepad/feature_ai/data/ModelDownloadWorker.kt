package com.mintanable.notethepad.feature_ai.data

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mintanable.notethepad.core.model.NoteThePadConstants.DOWNLOAD_MODEL_NOTIFICATION_CHANNEL_ID
import com.mintanable.notethepad.core.model.NoteThePadConstants.DOWNLOAD_MODEL_NOTIFICATION_ID
import com.mintanable.notethepad.core.analytics.AnalyticsEvent
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.feature_ai.R
import com.mintanable.notethepad.feature_ai.domain.DownloadCancelReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analyticsTracker: AnalyticsTracker
) : CoroutineWorker(context, params) {

    companion object {
        const val MODEL_DOWNLOAD_TASK = "model_download_task"
        const val MODEL_URL = "model_url"
        const val MODEL_FILE_NAME = "model_file_name"
        const val HF_TOKEN = "hf_token"
        const val DOWNLOAD_ID= "DOWNLOAD_ID"

    }

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    @SuppressLint("Range", "RestrictedApi")
    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val modelUrl = inputData.getString(MODEL_URL) ?: return Result.failure()
        val fileName = inputData.getString(MODEL_FILE_NAME) ?: return Result.failure()
        val token = inputData.getString(HF_TOKEN) ?: ""
        val expectedContentSize = getExpectedContentSize(modelUrl, token)

        Log.d("kptest", "modelUrl = $modelUrl, fileName = $fileName, token = $token, expectedContentSize = $expectedContentSize")

        val destinationFile = File(applicationContext.getExternalFilesDir(null), fileName)
        if (destinationFile.exists()) {
            Log.d("kptest", "Old file found, deleting to prevent naming conflicts: $fileName")
            destinationFile.delete()
        }

        val downloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val existingId = findActiveDownload(downloadManager, fileName)
        val id = existingId ?: run {
            val request = DownloadManager.Request(modelUrl.toUri())
                .setTitle("NoteThePad AI assistant Update")
                .setDescription("Downloading $fileName")
                .addRequestHeader("Authorization", "Bearer $token")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalFilesDir(applicationContext, null, fileName)
                .setAllowedOverMetered(false)
                .setAllowedOverRoaming(false)

            downloadManager.enqueue(request)
        }

        var isDownloading = true
        var result = Result.failure()

        while (isDownloading) {
            Log.d("kptest", "isDownloading")
            val query = DownloadManager.Query().setFilterById(id)
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isDownloading = false
                            result = Result.success()
                            Log.d("kptest", "STATUS_SUCCESSFUL")
                            setProgress(workDataOf("percent" to 100))
                        }

                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            val errorMessage = getDownloadErrorMessage(reason)
                            Log.e("kptest", "STATUS_FAILED: $errorMessage (Code: $reason)")
                            analyticsTracker.track(AnalyticsEvent.AiModelDownloadResult(fileName, false, System.currentTimeMillis() - startTime, errorMessage))
                            isDownloading = false
                            result = Result.failure(workDataOf("ERROR" to errorMessage))
                        }

                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                            val downloaded =
                                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val total =
                                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                            if (total > 0) {
                                val progress = ((downloaded * 100) / total).toInt()
                                Log.d("kptest", "Model download in progress: $progress")

                                setProgress(
                                    workDataOf(
                                        "percent" to progress,
                                        "fileName" to fileName
                                    )
                                )
                                setForeground(createForegroundInfo(downloadId = id, progress, fileName))
                            }
                        }
                    }
                } else {
                    isDownloading = false
                }
                cursor.close()
            }
            if (isDownloading) delay(1000)
        }

        if (result is Result.Success) {
            setForeground(createForegroundInfo(downloadId = id, 100, "Verifying integrity..."))
            val downloadedFile = File(applicationContext.getExternalFilesDir(null), fileName)

            val downloadedSize = withContext(Dispatchers.IO) {
                downloadedFile.length()
            }

            return if (downloadedSize == expectedContentSize) {
                Log.d("kptest", "Size match verified!")
                analyticsTracker.track(AnalyticsEvent.AiModelDownloadResult(fileName, true, System.currentTimeMillis() - startTime))
                Result.success()
            } else {
                Log.e("kptest", "File size mismatch! Actual: $downloadedSize, Expected: $expectedContentSize")
                analyticsTracker.track(AnalyticsEvent.AiModelDownloadResult(fileName, false, System.currentTimeMillis() - startTime, "size_mismatch"))
                downloadedFile.delete()
                Result.failure(workDataOf("ERROR" to "Checksum verification failed, File corrupted during download"))
            }
        }
        return result
    }

    private suspend fun getExpectedContentSize(url: String, token: String): Long = withContext(Dispatchers.IO) {
        val redirectClient = client.newBuilder()
            .followRedirects(true)
            .build()

        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .addHeader("Authorization", "Bearer $token")
                .build()

            redirectClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("kptest", "Failed to fetch metadata: Code ${response.code}")
                    return@withContext 0L
                }

                val size = response.header("Content-Length")?.toLongOrNull() ?: -1L
                Log.d("kptest", "Expected size: $size bytes")
                size
            }
        } catch (e: Exception) {
            Log.e("kptest", "Network Error: ${e.message}")
            -1L
        }
    }

    private fun createForegroundInfo(downloadId: Long, progress: Int, modelName: String): ForegroundInfo {
        var pendingCancelIntent: PendingIntent? = null

        if(downloadId > -1) {
            val cancelIntent = Intent(applicationContext, DownloadCancelReceiver::class.java).apply {
                putExtra(DOWNLOAD_ID, downloadId)
            }

            pendingCancelIntent = PendingIntent.getBroadcast(
               applicationContext,
               0,
               cancelIntent,
               PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
           )
       }
        val notification = NotificationCompat.Builder(applicationContext, DOWNLOAD_MODEL_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AI Model Sync")
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

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(-1,0,"")
    }

    @SuppressLint("Range")
    private fun findActiveDownload(dm: DownloadManager, fileName: String): Long? {
        val query = DownloadManager.Query().setFilterByStatus(
            DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_PENDING
        )
        dm.query(query)?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
                if (title.contains(fileName)) return cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
            }
        }
        return null
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192) // 8KB chunks
        file.inputStream().use { inputStream ->
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun getDownloadErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space on device"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_CANNOT_RESUME -> "Network connection lost and cannot resume"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error (Check URL)"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP error code"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "External storage not found"
            DownloadManager.ERROR_FILE_ERROR -> "Local file system error"
            else -> "Unknown error (Code $reason)"
        }
    }
}