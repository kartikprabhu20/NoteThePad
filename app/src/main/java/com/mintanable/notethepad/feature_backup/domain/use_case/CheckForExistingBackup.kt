package com.mintanable.notethepad.feature_backup.domain.use_case

import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CheckForExistingBackup@Inject constructor(
    private val googleDriveRepository: GoogleDriveRepository,
    private val googleAuthRepository: GoogleAuthRepository
) {
    suspend operator fun invoke(): Flow<DriveFileMetadata?> = flow {
        try {
            val refreshToken = googleAuthRepository.getDecryptedRefreshToken()

            if (refreshToken == null) {
                emit(null)
                return@flow
            }
            val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)
            emitAll(googleDriveRepository.checkForExistingBackup(accessToken))

        } catch (e: Exception) {
            emit(null)
        }
    }
}