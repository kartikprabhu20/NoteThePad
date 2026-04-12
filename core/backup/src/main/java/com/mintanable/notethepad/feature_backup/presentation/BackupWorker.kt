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
import com.mintanable.notethepad.core.model.NoteThePadConstants.BACKUP_NOTIFICATION_ID
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.database.db.DatabaseManager
import com.mintanable.notethepad.database.db.NoteDatabase
import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.core.analytics.AnalyticsEvent
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.feature_backup.R
import com.mintanable.notethepad.file.FileManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val googleAuthRepository: GoogleAuthRepository,
    private val driveRepository: GoogleDriveRepository,
    private val backupScheduler: BackupScheduler,
    private val userPrefs: UserPreferencesRepository,
    private val noteRepository: NoteRepository,
    private val fileManager: FileManager,
    private val dbManager: DatabaseManager,
    private val analyticsTracker: AnalyticsTracker
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO){
        val startTime = System.currentTimeMillis()
        val trigger = inputData.getString("trigger") ?: "scheduled"
        analyticsTracker.track(AnalyticsEvent.BackupStarted(trigger))
        try {
            val refreshToken =
                googleAuthRepository.getDecryptedRefreshToken() ?: return@withContext Result.failure()
            val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)
            val settings = userPrefs.settingsFlow.first()
            dbManager.closeDatabase()
            val dbFile = applicationContext.getDatabasePath(NoteDatabase.DATABASE_NAME)
            val filesToUpload = mutableListOf(dbFile to BACKUP_DB_FILE_NAME)

            if (settings.backupSettings.backupMedia) {
                val notes = noteRepository.getNotes(NoteOrder.Date(OrderType.Descending)).first()
                notes.flatMap { it.noteEntity.imageUris + it.noteEntity.audioUris }
                    .distinct()
                    .forEach { uriString ->
                        val file = fileManager.getFileFromUri(uriString)
                        file?.let { filesToUpload.add(file to "media_${file.name}") }
                    }
            }

            try {
                setForeground(createForegroundInfo(0))
            } catch (e: Exception) {
                Log.e("kptest", "Foreground service start denied. Running in background.")
            }

            var uploadSuccess = true
            driveRepository.uploadMultipleFiles(accessToken, filesToUpload).collect { status ->
                Log.d("kptest", "doWork status: $status")
                when (status) {
                    is LoadStatus.Progress -> {
                        setForeground(createForegroundInfo(status.percentage / 2))
                        setProgress(
                            workDataOf(
                                "percent" to status.percentage / 2,
                                "bytes" to status.bytes,
                                "totalBytes" to status.totalBytes
                            )
                        )
                    }

                    is LoadStatus.Success -> {
                        Log.d("kptest", "Backup success")
                    }

                    is LoadStatus.Error -> {
                        Log.d("kptest", "Backup failed")
                        uploadSuccess = false
                        return@collect
                    }

                    else -> {}
                }
            }

            if (!uploadSuccess) {
                analyticsTracker.track(AnalyticsEvent.BackupResult(false, System.currentTimeMillis() - startTime, runAttemptCount, "upload_failed"))
                return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
            }

            backupScheduler.onWorkCompleted(inputData)
            analyticsTracker.track(AnalyticsEvent.BackupResult(true, System.currentTimeMillis() - startTime, runAttemptCount))
            return@withContext Result.success()

        } catch (e: Exception) {
            Log.e("kptest", "Error: ${e.message}")
            analyticsTracker.track(AnalyticsEvent.BackupResult(false, System.currentTimeMillis() - startTime, runAttemptCount, e::class.simpleName))
            return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(applicationContext, BACKUP_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Cloud Backup")
                .setContentText("Uploading notes to Google Drive...")
                .setSmallIcon(R.drawable.ic_backup_notification)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                BACKUP_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0)
    }
}