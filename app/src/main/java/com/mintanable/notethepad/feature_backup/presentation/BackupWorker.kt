package com.mintanable.notethepad.feature_backup.presentation

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.feature_note.data.source.NoteDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.workDataOf
import com.mintanable.notethepad.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val googleAuthRepository: GoogleAuthRepository,
    private val driveRepository: GoogleDriveRepository,
    private val backupScheduler: BackupScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                setForeground(createForegroundInfo(0))

                val refreshToken =
                    googleAuthRepository.getDecryptedRefreshToken() ?: return@withContext Result.failure()
                val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)

                val dbFile = applicationContext.getDatabasePath(NoteDatabase.DATABASE_NAME)

                var finalResult: Result = Result.failure()
                driveRepository.uploadFileWithProgress(accessToken, dbFile).collect { status ->

                    Log.d("kptest", "doWork status: $status")

                    when (status) {
                        is BackupStatus.Progress -> {
                            setForeground(createForegroundInfo(status.percentage))
                            setProgress(workDataOf("percent" to status.percentage))
                        }
                        is BackupStatus.Success -> {
                            backupScheduler.onWorkCompleted(inputData)
                            finalResult = Result.success()
                        }
                        is BackupStatus.Error -> {
                            finalResult = if (runAttemptCount < 3) Result.retry() else Result.failure()
                        }
                        else -> {}
                    }
                }

                return@withContext finalResult
            } catch (e: Exception) {
                Log.e("kptest", "Error: ${e.message}")
                return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "backup_channel")
            .setContentTitle("Cloud Backup")
            .setContentText("Uploading notes to Google Drive...")
            .setSmallIcon(R.mipmap.notethepad_launcher_round)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(1001, notification)
        }
    }
}