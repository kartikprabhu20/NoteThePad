package com.mintanable.notethepad.feature_ai.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import android.util.Log
import com.mintanable.notethepad.core.common.utils.convertWavToMonoWithMaxSeconds
import com.mintanable.notethepad.core.common.utils.splitPcmIntoChunks
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import com.mintanable.notethepad.feature_ai.data.source.GeminiDataSource
import com.mintanable.notethepad.feature_ai.data.source.GeminiNanoDataSource
import com.mintanable.notethepad.feature_ai.data.source.GemmaLocalDataSource
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import java.io.File
import javax.inject.Inject

class NoteAssistantRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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

    private fun createTagPrompt(
        title: String,
        content: String,
        existingTags: List<String>
    ): String {
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

    private fun createFewShotTagPrompt(
        title: String,
        content: String,
        existingTags: List<String>
    ): String {
        val prompt = """
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
            
            Output:
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

    override suspend fun transcribeAudioFile(
        uri: String,
        modelName: String,
        onTranscription: (String) -> Unit
    ) {
        when (modelName) {
            "Gemini Nano (System)" -> {
                val path = uri.toUri().path ?: return
                geminiNanoDataSource.transcribeAudioFile(File(path), onTranscription)
            }

            "Gemini 3 Flash (Cloud)" -> { /* Handle Cloud if needed */
            }

            "None" -> {}

            else -> {
                // Gemma models
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }

                if (selectedModel != null) {
                    // Normalize audio to 16kHz mono 16-bit PCM (no duration limit)
                    val pcmData = convertWavToMonoWithMaxSeconds(
                        context = context,
                        stereoUri = uri.toUri(),
                        maxSeconds = Int.MAX_VALUE / 16000 // no trim — chunking handles it
                    ) ?: return

                    val chunks = splitPcmIntoChunks(pcmData, chunkDurationSeconds = 10)
                    Log.d("kptest", "Gemma audio: ${pcmData.size} bytes PCM, ${chunks.size} chunks")
                    try {
                        gemmaLocalDataSource.prepareForAudio(selectedModel)
                        for ((index, chunkWav) in chunks.withIndex()) {
                            val callback: (String) -> Unit = if (index == 0) {
                                onTranscription
                            } else {
                                { text -> onTranscription(" $text") }
                            }
                            gemmaLocalDataSource.transcribeAudioFile(chunkWav, callback)
                            gemmaLocalDataSource.resetConversation()
                        }
                    } catch (e: Exception) {
                        Log.e("kptest", "Batch failed", e)
                    } finally {
                        // Close the engine ONCE at the very end
                        gemmaLocalDataSource.cleanup()
                    }
                }
            }
        }
    }

    override suspend fun analyzeImage(imageBytes: ByteArray, modelName: String): List<String> {
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.analyzeImage(imageBytes)
            "Gemini Nano (System)" -> emptyList() // Nano doesn't support images
            "None" -> emptyList()
            else -> {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.llmSupportImage) {
                    val response = gemmaLocalDataSource.analyzeImage(
                        imageBytes,
                        selectedModel.downloadFileName
                    )
                    response?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(3)
                        ?: emptyList()
                } else {
                    emptyList()
                }
            }
        }
    }

    override suspend fun describeImage(imageBytes: ByteArray, modelName: String): String? {
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.describeImage(imageBytes)
            "Gemini Nano (System)" -> null // Nano doesn't support images
            "None" -> null
            else -> {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.llmSupportImage) {
                    gemmaLocalDataSource.describeImage(imageBytes, selectedModel.downloadFileName)
                } else null
            }
        }
    }

    override suspend fun summarizeNote(prompt: String, modelName: String): String? {
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.summarizeNote(prompt)
            "Gemini Nano (System)" -> geminiNanoDataSource.summarizeNote(prompt)
            "None" -> null
            else -> {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.isLlm) {
                    gemmaLocalDataSource.summarizeNote(prompt, selectedModel.downloadFileName)
                } else null
            }
        }
    }

    override fun queryImage(imageBytes: ByteArray, query: String, modelName: String): Flow<String> {
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.queryImage(imageBytes, query)
            "Gemini Nano (System)" -> emptyFlow()
            "None" -> emptyFlow()
            else -> flow {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.llmSupportImage) {
                    gemmaLocalDataSource.queryImage(
                        imageBytes,
                        query,
                        selectedModel.downloadFileName
                    )
                        .onCompletion { gemmaLocalDataSource.cleanup() }
                        .collect { emit(it) }
                }
            }
        }
    }

}
