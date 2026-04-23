package com.mintanable.notethepad.feature_settings.domain.use_case

import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.network.sync.SupabaseSyncService
import com.mintanable.notethepad.feature_backup.domain.use_case.ClearAppDataUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteAllCloudDataUseCase @Inject constructor(
    private val supabaseSyncService: SupabaseSyncService,
    private val authRepository: AuthRepository,
    private val clearAppDataUseCase: ClearAppDataUseCase,
) {
    suspend operator fun invoke(): Result<Unit> = try {
        val user = authRepository.getSignedInFirebaseUser().first()
            ?: return Result.failure(Exception("Please sign in first"))
        val token = authRepository.getFreshFirebaseToken()
            ?: return Result.failure(Exception("Authentication expired. Sign in again."))

        supabaseSyncService.ensureAuthenticated(token)

        supabaseSyncService.deleteAllUserData(user.uid)
            .onFailure { return Result.failure(it) }

        clearAppDataUseCase().getOrElse { err ->
            if (err.message?.contains("No Google account linked") == true) Unit
            else return Result.failure(err)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
