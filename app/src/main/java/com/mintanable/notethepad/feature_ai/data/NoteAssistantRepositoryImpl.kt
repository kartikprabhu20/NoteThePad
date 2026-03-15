package com.mintanable.notethepad.feature_ai.data

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.mintanable.notethepad.feature_ai.domain.model.AiModelDownloadStatus
import com.mintanable.notethepad.feature_ai.domain.NoteAssistantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NoteAssistantRepositoryImpl @Inject constructor(
private val geminiDataSource: GeminiDataSource
) : NoteAssistantRepository {

    override suspend fun suggestTagsFromCloud(title: String, content: String, existingTags: List<String>): List<String> {
        return geminiDataSource.generateTags(title, content, existingTags)
    }

    override fun checkLocalStatus(): Flow<AiModelDownloadStatus> = flow {
        val status = Generation.getClient().checkStatus()
        val mappedStatus = when (status) {
            FeatureStatus.AVAILABLE -> AiModelDownloadStatus.Ready
            FeatureStatus.DOWNLOADING -> AiModelDownloadStatus.Downloading
            FeatureStatus.DOWNLOADABLE -> AiModelDownloadStatus.Downloadable
            else -> AiModelDownloadStatus.Unavailable
        }
        emit(mappedStatus)
    }
}