package com.mintanable.notethepad.feature_ai.domain.use_cases

import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class QueryImageUseCase @Inject constructor(
    private val noteAssistantRepository: NoteAssistantRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray, query: String): Flow<String> {
        val settings = userPreferencesRepository.settingsFlow.first()
        return noteAssistantRepository.queryImage(imageBytes, query, settings.aiModelName)
    }
}
