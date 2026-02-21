package com.mintanable.notethepad.feature_backup.domain.repository

import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationResult

interface DriveRepository {
    suspend fun authorizeDriveAccess(accountEmail: String): Result<AuthorizationResult>
    suspend fun exchangeCodeForTokens(authCode: String): Result<Unit>
    suspend fun hasDriveAccess(): Boolean
    suspend fun getDecryptedRefreshToken(): String?
    suspend fun getAuthCodeFromIntent(data: Intent?): String?
    suspend fun clearDriveCredentials()
}