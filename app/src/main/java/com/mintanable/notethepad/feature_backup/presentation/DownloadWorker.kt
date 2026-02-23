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
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.feature_note.data.source.DatabaseManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class DownloadWorker@AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val googleDriveRepository: GoogleDriveRepository,
    private val googleAuthRepository: GoogleAuthRepository,
    private val dbManager: DatabaseManager
    ): CoroutineWorker(appContext, workerParams){

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                setForeground(createForegroundInfo(0))

                val refreshToken = googleAuthRepository.getDecryptedRefreshToken()
                    ?: throw Exception("No Google account linked")

                val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)
                val tempFile = File(applicationContext.cacheDir, "temp_restore.db")

                var finalResult: Result = Result.failure()

                googleDriveRepository.downloadBackupFile(accessToken, tempFile).collect { status ->
                    Log.d("kptest", "doWork Download status: $status")
                    when (status) {
                        is BackupStatus.Progress -> {
                            setForeground(createForegroundInfo(status.percentage))
                            setProgress(workDataOf("percent" to status.percentage))
                        }
                        is BackupStatus.Success -> {
                            dbManager.swapDatabase(tempFile)
                            finalResult = Result.success()
                        }
                        is BackupStatus.Error -> {
                            finalResult = Result.failure()
                        }
                        else -> {}
                    }
                }
                return@withContext finalResult
            } catch (e: Exception) {
                Result.failure()
                return@withContext Result.failure()
            }
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "backup_channel")
            .setContentTitle("Cloud Backup Restore")
            .setContentText("Downloading notes from Google Drive...")
            .setSmallIcon(R.mipmap.notethepad_launcher_round)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                1002,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(1002, notification)
        }
    }
}