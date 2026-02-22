package com.mintanable.notethepad.feature_backup.presentation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mintanable.notethepad.feature_backup.domain.BackupScheduler
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.feature_note.data.source.NoteDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val googleAuthRepository: GoogleAuthRepository,
    private val driveRepository: GoogleDriveRepository,
    private val backupScheduler: BackupScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val refreshToken = googleAuthRepository.getDecryptedRefreshToken() ?: return Result.failure()
            val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)

            val dbFile = applicationContext.getDatabasePath(NoteDatabase.DATABASE_NAME)
            driveRepository.uploadFileWithProgress(accessToken, dbFile)

            backupScheduler.onWorkCompleted(inputData)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}