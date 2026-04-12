package com.mintanable.notethepad.feature_backup.presentation

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mintanable.notethepad.core.model.NoteThePadConstants.BACKUP_DB_FILE_NAME
import com.mintanable.notethepad.core.model.backup.LoadStatus
import com.mintanable.notethepad.core.model.NoteThePadConstants.BACKUP_NOTIFICATION_CHANNEL_ID
import com.mintanable.notethepad.core.model.NoteThePadConstants.DOWNLOAD_NOTIFICATION_ID
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.core.analytics.AnalyticsEvent
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.feature_backup.R
import com.mintanable.notethepad.file.FileManager
import org.json.JSONObject
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.delay

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val googleDriveRepository: GoogleDriveRepository,
    private val googleAuthRepository: GoogleAuthRepository,
    private val noteRepository: NoteRepository,
    private val fileManager: FileManager,
    private val analyticsTracker: AnalyticsTracker
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        analyticsTracker.track(AnalyticsEvent.RestoreStarted)
        try {
            setForeground(createForegroundInfo(0))

            val refreshToken = googleAuthRepository.getDecryptedRefreshToken()
                ?: throw Exception("No Google account linked")
            val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)
            val tempDBFile = File(applicationContext.cacheDir, "temp_restore.db")
            if (tempDBFile.exists()) {
                val deleted = tempDBFile.delete()
                Log.d("kptest", "Deleted old temp file: $deleted")
            }
            var downloadSuccess = true

            //Download DB
            var dbDownloaded = false
            googleDriveRepository.downloadFile(accessToken, tempDBFile, BACKUP_DB_FILE_NAME)
                .collect { status ->
                    when (status) {
                        is LoadStatus.Progress -> setProgress(workDataOf("percent" to status.percentage / 2))
                        is LoadStatus.Success -> dbDownloaded = true
                        is LoadStatus.Error -> Log.e(
                            "kptest",
                            "DB Download Error: ${status.message}"
                        )

                        else -> Unit
                    }
                }

            if (!dbDownloaded || !tempDBFile.exists()) {
                analyticsTracker.track(AnalyticsEvent.RestoreResult(false, System.currentTimeMillis() - startTime, "db_download_failed"))
                return@withContext Result.failure()
            }

            noteRepository.swapDatabase(tempDBFile)
            delay(1000) // Give the system a moment to realize the file handles have changed

            //Download Media
            // Fetch notes again after DB swap to get media URIs
            try {
                val notes = withContext(Dispatchers.IO) {
                    noteRepository.getNotes(NoteOrder.Date(OrderType.Descending)).first()
                }

                val mediaToDownload =
                    notes.flatMap { it.noteEntity.imageUris + it.noteEntity.audioUris }
                        .distinct()
                        .mapNotNull { path ->
                            val fileName =
                                File(path).name.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val file = File(fileManager.getMediaDir(), fileName)
                            file to "media_$fileName"
                        }

                if (mediaToDownload.isNotEmpty()) {
                    googleDriveRepository.downloadMultipleFiles(accessToken, mediaToDownload)
                        .collect { status ->
                            Log.d("kptest", "doWork status: $status")
                            when (status) {
                                is LoadStatus.Progress -> {
                                    val totalProgress = 50 + (status.percentage / 2)
                                    setForeground(createForegroundInfo(totalProgress))
                                    setProgress(
                                        workDataOf(
                                            "percent" to totalProgress,
                                            "bytes" to status.bytes,
                                            "totalBytes" to status.totalBytes
                                        )
                                    )
                                }

                                is LoadStatus.Success -> {
                                    Log.d("kptest", "Media downloaded successfully")
                                }

                                is LoadStatus.Error -> {
                                    Log.d("kptest", "Media download failed")
                                    downloadSuccess = false
                                    return@collect
                                }

                                else -> {}
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e(
                    "kptest",
                    "Media restoration encountered an error, but DB is safe: ${e.message}"
                )
                // We return success because the Database (the notes) are already swapped!
                analyticsTracker.track(AnalyticsEvent.RestoreResult(true, System.currentTimeMillis() - startTime, "media_partial"))
                return@withContext Result.success()
            }
            try {
                remapMediaPaths()
                Log.d("kptest", "Media paths remapped successfully")
            } catch (e: Exception) {
                Log.e("kptest", "Error remapping media paths: ${e.message}")
            }

            setForeground(createForegroundInfo(100))
            setProgress(workDataOf("percent" to 100))

            if (!downloadSuccess) {
                analyticsTracker.track(AnalyticsEvent.RestoreResult(false, System.currentTimeMillis() - startTime, "media_download_failed"))
                return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
            }

            analyticsTracker.track(AnalyticsEvent.RestoreResult(true, System.currentTimeMillis() - startTime))
            return@withContext Result.success()
        } catch (e: Exception) {
            analyticsTracker.track(AnalyticsEvent.RestoreResult(false, System.currentTimeMillis() - startTime, e::class.simpleName))
            return@withContext Result.failure()
        }
    }

    private suspend fun remapMediaPaths() {
        val currentMediaDir = fileManager.getMediaDir()
        val notes = noteRepository.getNotesWithMedia()

        for (note in notes) {
            var changed = false

            val newImageUris = note.imageUris.map { oldPath ->
                val fileName = File(oldPath).name
                val newFile = File(currentMediaDir, fileName)
                if (newFile.absolutePath != oldPath && newFile.exists()) {
                    changed = true
                    newFile.absolutePath
                } else {
                    oldPath
                }
            }

            val newAudioUris = note.audioUris.map { oldPath ->
                val fileName = File(oldPath).name
                val newFile = File(currentMediaDir, fileName)
                if (newFile.absolutePath != oldPath && newFile.exists()) {
                    changed = true
                    newFile.absolutePath
                } else {
                    oldPath
                }
            }

            var newTranscriptions = note.audioTranscriptions
            if (note.audioTranscriptions.isNotBlank()) {
                try {
                    val oldJson = JSONObject(note.audioTranscriptions)
                    val newJson = JSONObject()
                    oldJson.keys().forEach { oldKey ->
                        val fileName = File(oldKey).name
                        val newFile = File(currentMediaDir, fileName)
                        val newKey = if (newFile.absolutePath != oldKey && newFile.exists()) {
                            changed = true
                            newFile.absolutePath
                        } else {
                            oldKey
                        }
                        newJson.put(newKey, oldJson.getString(oldKey))
                    }
                    newTranscriptions = newJson.toString()
                } catch (e: Exception) {
                    Log.e("kptest", "Error remapping transcriptions for note ${note.id}: ${e.message}")
                }
            }

            if (changed) {
                noteRepository.updateNoteEntity(
                    note.copy(
                        imageUris = newImageUris,
                        audioUris = newAudioUris,
                        audioTranscriptions = newTranscriptions
                    )
                )
            }
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(applicationContext, BACKUP_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Cloud Backup Restore")
                .setContentText("Downloading notes from Google Drive...")
                .setSmallIcon(R.drawable.ic_backup_notification)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                DOWNLOAD_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(DOWNLOAD_NOTIFICATION_ID, notification)
        }
    }
}