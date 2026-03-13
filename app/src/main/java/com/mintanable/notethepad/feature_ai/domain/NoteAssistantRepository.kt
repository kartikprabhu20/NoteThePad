package com.mintanable.notethepad.feature_ai.domain

interface NoteAssistantRepository {
    suspend fun suggestTags(title: String, content: String, existingTags: List<String>): List<String>
}