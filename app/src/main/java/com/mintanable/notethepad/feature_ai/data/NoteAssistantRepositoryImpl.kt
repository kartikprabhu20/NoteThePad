package com.mintanable.notethepad.feature_ai.data

import com.mintanable.notethepad.feature_ai.domain.NoteAssistantRepository
import javax.inject.Inject

class NoteAssistantRepositoryImpl @Inject constructor(
private val geminiService: GeminiService
) : NoteAssistantRepository {

    override suspend fun suggestTags(title: String, content: String, existingTags: List<String>): List<String> {
        return geminiService.generateTags(title, content, existingTags)
    }
}