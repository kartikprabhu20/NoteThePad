package com.mintanable.notethepad.feature_ai.data

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import com.mintanable.notethepad.NoteThePadApp.Companion.DOWNLOAD_MODEL_NOTIFICATION_ID
import com.mintanable.notethepad.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val MODEL_DOWNLOAD_TASK = "model_download_task"
        const val MODEL_URL = "model_url"
        const val MODEL_FILE_NAME = "model_file_name"

    }

    @SuppressLint("Range", "RestrictedApi")
    override suspend fun doWork(): Result {
        val modelUrl = inputData.getString(MODEL_URL) ?: return Result.failure()
        val fileName = inputData.getString(MODEL_FILE_NAME) ?: return Result.failure()

        val downloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val existingId = findActiveDownload(downloadManager, fileName)
        val id = existingId ?: run {
            val request = DownloadManager.Request(modelUrl.toUri())
                .setTitle("NoteThePad AI Update\"")
                .setDescription("Downloading $fileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(applicationContext, null, fileName)
            downloadManager.enqueue(request)
        }

        var isDownloading = true
        var result = Result.failure()

        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(id)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                when(status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        isDownloading = false
                        result = Result.success()
                        setProgress(workDataOf("percent" to 100))
                    }
                    DownloadManager.STATUS_FAILED -> {
                        isDownloading = false
                        result = Result.failure()
                    }
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                        val downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        if (total > 0) {
                            val progress = ((downloaded * 100) / total).toInt()
                            setProgress(workDataOf("percent" to progress))
                            setForeground(createForegroundInfo(progress, fileName))
                        }
                    }
                }
                cursor.close()
            } else {
                isDownloading = false
            }
            if (isDownloading) delay(1000)
        }

        if (result is Result.Success) {
            val downloadedFile = File(applicationContext.getExternalFilesDir(null), fileName)
            val expectedHash = inputData.getString("EXPECTED_HASH") ?: return Result.success()

            setForeground(createForegroundInfo(100, "Verifying integrity..."))
            val actualHash = withContext(Dispatchers.IO) {
                calculateSHA256(downloadedFile)
            }

            return if (actualHash.equals(expectedHash, ignoreCase = true)) {
                Log.d("kptest", "Checksum verified!")
                Result.success()
            } else {
                Log.e("kptest", "Checksum mismatch! Expected: $expectedHash, Actual: $actualHash")
                downloadedFile.delete() // Delete corrupted file
                Result.failure(workDataOf("ERROR" to "File corrupted during download"))
            }
        }

        return result
    }

    private fun createForegroundInfo(progress: Int, modelName: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "AI_DOWNLOAD_CHANNEL")
            .setContentTitle("AI Model Sync")
            .setContentText("Downloading $modelName: $progress%")
            .setSmallIcon(R.drawable.notethepad_launcher_foreground)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(DOWNLOAD_MODEL_NOTIFICATION_ID, notification)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0,"")
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
}