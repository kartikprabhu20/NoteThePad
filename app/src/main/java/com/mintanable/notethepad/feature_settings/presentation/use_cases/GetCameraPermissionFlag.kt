package com.mintanable.notethepad.feature_settings.presentation.use_cases

import com.mintanable.notethepad.feature_settings.data.repository.SharedPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetCameraPermissionFlag @Inject constructor(
    private val repository: SharedPreferencesRepository
) {
    suspend operator fun invoke(): Boolean = repository.askedCameraPermission.first()
}