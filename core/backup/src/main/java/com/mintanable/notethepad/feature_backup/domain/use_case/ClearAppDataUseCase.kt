package com.mintanable.notethepad.feature_backup.domain.use_case

import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import javax.inject.Inject

class ClearAppDataUseCase @Inject constructor(
    private val driveRepository: GoogleDriveRepository,
    private val googleAuthRepository: GoogleAuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val refreshToken = googleAuthRepository.getDecryptedRefreshToken()
                ?: return Result.failure(Exception("No Google account linked"))
            val accessToken = googleAuthRepository.refreshAccessToken(refreshToken)
            driveRepository.clearAppDataFolder(accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
