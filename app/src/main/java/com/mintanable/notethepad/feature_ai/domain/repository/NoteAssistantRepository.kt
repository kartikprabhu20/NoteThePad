package com.mintanable.notethepad.feature_ai.domain.repository

import com.mintanable.notethepad.feature_ai.domain.model.AiModelDownloadStatus
import kotlinx.coroutines.flow.Flow

interface NoteAssistantRepository {
    suspend fun suggestTags(
        title: String,
        content: String,
        existingTags: List<String>,
        modelName: String
    ): List<String>

    suspend fun checkLocalStatus(modelName: String): Flow<AiModelDownloadStatus>
}
