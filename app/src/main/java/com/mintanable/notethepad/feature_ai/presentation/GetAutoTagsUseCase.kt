package com.mintanable.notethepad.feature_ai.presentation

import android.util.Log
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_settings.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetAutoTagsUseCase @Inject constructor(
    private val assistantRepository: NoteAssistantRepository,
    private val noteRepository: NoteRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(title: String, content: String): Result<List<String>> {
        return try {
            val settings = userPreferencesRepository.settingsFlow.first()
            val existingTags = noteRepository.getAllTags().first().map { it.tagName }

            val suggestions = assistantRepository.suggestTags(
                title = title,
                content = content,
                existingTags = existingTags,
                modelName = settings.aiModelName
            )
            Log.d("kptest", "suggested tags: $suggestions")
            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}