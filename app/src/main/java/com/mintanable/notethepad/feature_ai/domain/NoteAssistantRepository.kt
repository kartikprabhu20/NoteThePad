package com.mintanable.notethepad.feature_ai.domain

import com.mintanable.notethepad.feature_ai.domain.model.AiModelDownloadStatus
import kotlinx.coroutines.flow.Flow

interface NoteAssistantRepository {
    suspend fun suggestTagsFromCloud(title: String, content: String, existingTags: List<String>): List<String>
    fun checkLocalStatus(): Flow<AiModelDownloadStatus>
}