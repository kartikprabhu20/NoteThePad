package com.mintanable.notethepad.feature_settings.domain.use_case

import com.mintanable.notethepad.database.preference.repository.SharedPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetCameraPermissionFlag @Inject constructor(
    private val repository: SharedPreferencesRepository
) {
    suspend operator fun invoke(): Boolean = repository.askedCameraPermission.first()
}
