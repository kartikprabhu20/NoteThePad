package com.mintanable.notethepad.feature_ai.domain.repository

import com.google.ai.edge.litertlm.ToolSet
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import kotlinx.coroutines.flow.Flow

interface NoteAssistantRepository {
    suspend fun suggestTags(
        title: String,
        content: String,
        existingTags: List<String>,
        modelName: String
    ): List<String>

    suspend fun checkLocalStatus(modelName: String): Flow<AiModelDownloadStatus>
    suspend fun startLiveTranscription(onTranscription: (String) -> Unit)
    suspend fun stopLiveTranscription()

    suspend fun checkAudioTransciberStatus(modelName: String): Flow<AiModelDownloadStatus>
    suspend fun  transcribeAudioFile(uri: String, modelName: String, onTranscription: (String) -> Unit)

    suspend fun analyzeImage(imageBytes: ByteArray, modelName: String): List<String>
    fun queryImage(imageBytes: ByteArray, query: String, modelName: String): Flow<String>
    suspend fun describeImage(imageBytes: ByteArray, modelName: String): String?

    suspend fun summarizeNote(prompt: String, modelName: String, tools: List<ToolSet>): String?
}
