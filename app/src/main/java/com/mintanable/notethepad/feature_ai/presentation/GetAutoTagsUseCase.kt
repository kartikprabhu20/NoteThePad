package com.mintanable.notethepad.feature_ai.presentation

import com.mintanable.notethepad.feature_ai.domain.NoteAssistantRepository
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetAutoTagsUseCase @Inject constructor(
    private val assistantRepository: NoteAssistantRepository,
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(title: String, content: String): Result<List<String>> {
        return try {
            val existingTags = noteRepository.getAllTags().first().map { it.tagName }

            val suggestions = assistantRepository.suggestTags(
                title = title,
                content = content,
                existingTags = existingTags
            )

            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
