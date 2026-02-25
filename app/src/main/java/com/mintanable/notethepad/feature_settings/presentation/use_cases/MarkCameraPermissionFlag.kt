package com.mintanable.notethepad.feature_settings.presentation.use_cases

import com.mintanable.notethepad.feature_settings.data.repository.SharedPreferencesRepository
import javax.inject.Inject

class MarkCameraPermissionFlag @Inject constructor(
    private val sharedPreferencesRepository: SharedPreferencesRepository
) {
    suspend operator fun invoke(){
        sharedPreferencesRepository.markCameraPermissionRequested()
    }
}