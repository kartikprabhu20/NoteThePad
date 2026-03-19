package com.mintanable.notethepad.feature_settings.domain.use_case

import com.mintanable.notethepad.database.preference.repository.SharedPreferencesRepository
import javax.inject.Inject

class MarkMicrophonePermissionFlag @Inject constructor(
    private val sharedPreferencesRepository: SharedPreferencesRepository
) {
    suspend operator fun invoke() {
        sharedPreferencesRepository.markMicrophonePermissionRequested()
    }
}
