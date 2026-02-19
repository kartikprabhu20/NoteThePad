package com.mintanable.notethepad.feature_settings.domain.use_case

import com.mintanable.notethepad.feature_settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetLayoutSettings @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> {
         return repository.settingsFlow
             .map { it.isGridViewSelected }
             .distinctUntilChanged()
    }
}