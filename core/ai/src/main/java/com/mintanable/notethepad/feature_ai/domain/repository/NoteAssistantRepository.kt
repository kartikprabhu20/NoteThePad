package com.mintanable.notethepad.feature_ai.domain.repository

import android.net.Uri
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

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
    suspend fun transcribeAudioFile(uri: Uri, modelName: String, onTranscription: (String) -> Unit)
}
