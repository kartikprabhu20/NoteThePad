package com.mintanable.notethepad.feature_ai.data.source

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.mintanable.notethepad.core.model.AiModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GeminiNanoDataSource @Inject constructor() {
    private val client = Generation.getClient()

    suspend fun generateTags(prompt: String): String? {
        return try {
            client.generateContent(prompt).candidates.firstOrNull()?.text
        } catch (e: Exception) {
            null
        }
    }

    fun checkLocalStatus(): Flow<AiModelDownloadStatus> = flow {
        val status = client.checkStatus()
        val mappedStatus = when (status) {
            FeatureStatus.AVAILABLE -> AiModelDownloadStatus.Ready
            FeatureStatus.DOWNLOADING -> AiModelDownloadStatus.Downloading
            FeatureStatus.DOWNLOADABLE -> AiModelDownloadStatus.Downloadable
            else -> AiModelDownloadStatus.Unavailable
        }
        emit(mappedStatus)
    }
}
