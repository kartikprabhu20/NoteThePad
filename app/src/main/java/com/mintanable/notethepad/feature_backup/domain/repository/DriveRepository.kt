package com.mintanable.notethepad.feature_backup.domain.repository

import com.google.android.gms.auth.api.identity.AuthorizationResult

interface DriveRepository {
    suspend fun authorizeDriveAccess(): Result<AuthorizationResult>
    suspend fun exchangeCodeForTokens(authCode: String): Result<Unit>
}