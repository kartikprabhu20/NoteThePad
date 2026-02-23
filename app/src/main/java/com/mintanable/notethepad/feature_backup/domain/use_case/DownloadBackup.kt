package com.mintanable.notethepad.feature_backup.domain.use_case

import android.content.Context
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_note.data.source.DatabaseManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class DownloadBackup @Inject constructor(
    private val googleDriveRepository: GoogleDriveRepository,
    private val googleAuthRepository: GoogleAuthRepository,
    private val dbManager: DatabaseManager,
    @ApplicationContext private val appContext: Context,
) {

    operator fun invoke(): Flow<BackupStatus> = flow {
        try {
            val refreshToken = googleAuthRepository.getDecryptedRefreshToken()
                ?: throw Exception("No Google account linked")

            val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)
            val tempFile = File(appContext.cacheDir, "temp_restore.db")

            googleDriveRepository.downloadBackupFile(accessToken, tempFile).collect { status ->
                if (status is BackupStatus.Success) {
                    dbManager.swapDatabase(tempFile)
                    emit(BackupStatus.Success)
                } else {
                    emit(status)
                }
            }
        } catch (e: Exception) {
            emit(BackupStatus.Error(e.message ?: "Restore failed"))
        }
    }
}