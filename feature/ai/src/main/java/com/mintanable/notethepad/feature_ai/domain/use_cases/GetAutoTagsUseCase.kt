package com.mintanable.notethepad.feature_ai.domain.use_cases

import android.util.Log
import com.mintanable.notethepad.database.repository.NoteRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import com.mintanable.notethepad.preference.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAutoTagsUseCase @Inject constructor(
    private val assistantRepository: NoteAssistantRepository,
    private val noteRepository: NoteRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(title: String, content: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val settings = userPreferencesRepository.settingsFlow.first()
                val existingTags = noteRepository.getAllTags().first().map { it.tagName }

                Log.d("GetAutoTagsUseCase", "Generating tags using: ${settings.aiModelName}")
                val suggestions = assistantRepository.suggestTags(
                    title = title,
                    content = content,
                    existingTags = existingTags,
                    modelName = settings.aiModelName
                )
                Log.d("GetAutoTagsUseCase", "suggested tags: $suggestions")
                Result.success(suggestions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}