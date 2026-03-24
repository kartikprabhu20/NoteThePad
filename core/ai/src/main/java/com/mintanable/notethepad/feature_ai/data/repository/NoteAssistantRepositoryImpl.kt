package com.mintanable.notethepad.feature_ai.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import com.mintanable.notethepad.feature_ai.data.source.GeminiDataSource
import com.mintanable.notethepad.feature_ai.data.source.GeminiNanoDataSource
import com.mintanable.notethepad.feature_ai.data.source.GemmaLocalDataSource
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class NoteAssistantRepositoryImpl @Inject constructor(
    private val geminiDataSource: GeminiDataSource,
    private val gemmaLocalDataSource: GemmaLocalDataSource,
    private val geminiNanoDataSource: GeminiNanoDataSource,
    private val aiModelRepository: AiModelRepository,
) : NoteAssistantRepository {

    override suspend fun suggestTags(
        title: String,
        content: String,
        existingTags: List<String>,
        modelName: String
    ): List<String> {
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.generateTags(title, content, existingTags)
            "Gemini Nano (System)" -> {
                val prompt = createTagPrompt(title, content, existingTags)
                val response = geminiNanoDataSource.generateTags(prompt)
                parseResponse(response)
            }

            "None" -> emptyList()
            else -> {
                //Gemma models
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.url.isNotEmpty()) {
                    val prompt = createFewShotTagPrompt(title, content, existingTags)
                    val response =
                        gemmaLocalDataSource.generateTags(prompt, selectedModel.downloadFileName)
                    parseResponse(response)
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun createTagPrompt(title: String, content: String, existingTags: List<String>): String {
        return """
            You are an expert organizational assistant for the app "NoteThePad".
            
            TASK: Analyze the note below and suggest 3-5 relevant one-word tags.
            
            CONTEXT:
            - Note Title: $title
            - Note Content: $content
            - User's Existing Tags: ${existingTags.joinToString(", ")}
            
            CRITICAL RULES:
            1. If a suggested tag matches an 'Existing Tag' semantically, use the EXACT name from the existing list.
            2. Return ONLY a comma-separated list of words.
            3. No hashtags, no explanations.
            
            Example Output: Work, Finance, Urgent
        """.trimIndent()
    }

    private fun createFewShotTagPrompt(title: String, content: String, existingTags: List<String>): String {
        val prompt = """
            NOTE DATA:
            - Title: $title
            - Content: $content
            - Existing Tags: ${existingTags.joinToString(", ")}
    
            Tags:
            """.trimIndent()
        return prompt
    }

    private fun parseResponse(response: String?): List<String> {
        return response?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    override suspend fun checkLocalStatus(modelName: String): Flow<AiModelDownloadStatus> =
        if (modelName == "Gemini Nano (System)") {
            geminiNanoDataSource.checkLocalStatus()
        } else {
            gemmaLocalDataSource.checkLocalStatus(aiModelRepository.getModelByName(modelName))
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override suspend fun startLiveTranscription(onTranscription: (String) -> Unit) {
        return geminiNanoDataSource.startTranscriptionStream(onTranscription)
    }

    override suspend fun stopLiveTranscription() {
        return geminiNanoDataSource.stopTranscription()
    }

    override suspend fun checkAudioTransciberStatus(modelName: String): Flow<AiModelDownloadStatus> {
        return geminiNanoDataSource.checkAudioRecognizerStatus()
    }

}
