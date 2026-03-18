package com.mintanable.notethepad.feature_settings.domain.use_case

import com.mintanable.notethepad.preference.repository.SharedPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetMicrophonePermissionFlag @Inject constructor(
    private val repository: SharedPreferencesRepository
) {
    suspend operator fun invoke(): Boolean = repository.askedMicrophonePermission.first()
}
