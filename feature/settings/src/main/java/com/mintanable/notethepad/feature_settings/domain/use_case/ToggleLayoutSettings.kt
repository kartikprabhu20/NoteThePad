package com.mintanable.notethepad.feature_settings.domain.use_case

import com.mintanable.notethepad.preference.repository.UserPreferencesRepository
import javax.inject.Inject

class ToggleLayoutSettings @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.gridviewEnabled(enabled)
    }
}
