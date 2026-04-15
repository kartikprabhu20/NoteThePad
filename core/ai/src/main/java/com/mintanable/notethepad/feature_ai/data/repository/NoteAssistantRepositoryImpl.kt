package com.mintanable.notethepad.feature_ai.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import android.util.Log
import com.google.ai.edge.litertlm.ToolSet
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mintanable.notethepad.core.common.utils.convertWavToMonoWithMaxSeconds
import com.mintanable.notethepad.core.common.utils.splitPcmIntoChunks
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import com.mintanable.notethepad.feature_ai.data.source.GeminiDataSource
import com.mintanable.notethepad.feature_ai.data.source.GeminiNanoDataSource
import com.mintanable.notethepad.feature_ai.data.source.GemmaLocalDataSource
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import com.mintanable.notethepad.feature_ai.tools.BackupTools
import com.mintanable.notethepad.feature_ai.tools.MediaTools
import com.mintanable.notethepad.feature_ai.tools.NoteTools
import com.mintanable.notethepad.feature_ai.tools.SearchTools
import com.mintanable.notethepad.feature_ai.tools.TagTools
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
    private val noteTools: NoteTools,
    private val searchTools: SearchTools,
    private val tagTools: TagTools,
    private val mediaTools: MediaTools,
    private val backupTools: BackupTools,
) : NoteAssistantRepository {

    private val stableTools: List<ToolSet>
        get() = listOf(noteTools, searchTools, tagTools, mediaTools, backupTools)

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override suspend fun suggestTags(
        title: String,
        content: String,
        existingTags: List<String>,
        modelName: String
    ): List<String> {
        crashlytics.log("NoteAssistantRepositoryImpl: Suggesting tags. Model: $modelName")
        crashlytics.setCustomKey("last_ai_model", modelName)

        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.generateTags(title, content, existingTags)
            "Gemini Nano (System)" -> {
                val prompt = createTagPrompt(title, content, existingTags)
                val response = geminiNanoDataSource.generateTags(prompt)
                parseResponse(response)
            }

            "None" -> emptyList()
            else -> {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.url.isNotEmpty()) {
                    val prompt = createFewShotTagPrompt(title, content, existingTags)
                    val response =
                        gemmaLocalDataSource.generateTags(prompt, selectedModel.downloadFileName)
                    parseResponse(response)
                } else {
                    crashlytics.log("NoteAssistantRepositoryImpl: Gemma model selection failed or URL empty")
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
            Output:
            """.trimIndent()
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
    override suspend fun startLiveTranscription(
        onTranscription: (String) -> Unit,
        aiModelName: String
    ) {
        crashlytics.log("NoteAssistantRepositoryImpl: Starting Live Transcription")

        when (aiModelName) {
            "Gemini 3 Flash (Cloud)", "None" -> return
            "Gemini Nano (System)" -> {
                return geminiNanoDataSource.startTranscriptionStream(onTranscription)
            }
            else -> return gemmaLocalDataSource.startTranscriptionStream(onTranscription)
        }
    }

    override suspend fun stopLiveTranscription(aiModelName: String) {
        crashlytics.log("NoteAssistantRepositoryImpl: Stopping Live Transcription")
        when (aiModelName) {
            "Gemini 3 Flash (Cloud)", "None" -> return
            "Gemini Nano (System)" -> {
                return geminiNanoDataSource.stopTranscription()
            }

            else -> return gemmaLocalDataSource.stopTranscription()
        }
    }

    override suspend fun checkAudioTransciberStatus(modelName: String): Flow<AiModelDownloadStatus> {
        return geminiNanoDataSource.checkAudioRecognizerStatus()
    }

    override suspend fun transcribeAudioFile(
        uri: String,
        modelName: String,
        onTranscription: (String) -> Unit
    ) {
        crashlytics.log("NoteAssistantRepositoryImpl: Transcribe file request. Model: $modelName")
        when (modelName) {
            "Gemini Nano (System)" -> {
                val path = uri.toUri().path ?: return
                geminiNanoDataSource.transcribeAudioFile(File(path), onTranscription)
            }
            "Gemini 3 Flash (Cloud)" -> { }
            "None" -> {}
            else -> {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }

                if (selectedModel != null) {
                    val pcmData = convertWavToMonoWithMaxSeconds(
                        context = context,
                        stereoUri = uri.toUri(),
                        maxSeconds = Int.MAX_VALUE / 16000
                    ) ?: run {
                        crashlytics.log("NoteAssistantRepositoryImpl: PCM conversion returned null")
                        return
                    }

                    val chunks = splitPcmIntoChunks(pcmData, chunkDurationSeconds = 10)
                    crashlytics.setCustomKey("audio_chunks", chunks.size)

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
                        crashlytics.log("NoteAssistantRepositoryImpl: Error during chunked transcription")
                        crashlytics.recordException(e)
                        Log.e("kptest", "Batch failed", e)
                    } finally {
                        gemmaLocalDataSource.cleanup()
                    }
                }
            }
        }
    }

    override suspend fun analyzeImage(imageBytes: ByteArray, modelName: String): List<String> {
        crashlytics.log("NoteAssistantRepositoryImpl: Analyze Image. Bytes: ${imageBytes.size}")
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.analyzeImage(imageBytes)
            "Gemini Nano (System)" -> emptyList()
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
        crashlytics.log("NoteAssistantRepositoryImpl: Describe Image. Bytes: ${imageBytes.size}")
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.describeImage(imageBytes)
            "Gemini Nano (System)" -> null
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

    override suspend fun summarizeNote(
        prompt: String,
        modelName: String,
        tools: List<ToolSet>
    ): String? {
        crashlytics.log("NoteAssistantRepositoryImpl: Summarize. Length: ${prompt.length}")
        val composedTools = tools + stableTools
        return when (modelName) {
            "Gemini 3 Flash (Cloud)" -> geminiDataSource.summarizeNote(prompt)
            "Gemini Nano (System)" -> geminiNanoDataSource.summarizeNote(prompt)
            "None" -> null
            else -> {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.isLlm) {
                    gemmaLocalDataSource.summarizeNote(
                        prompt,
                        selectedModel.downloadFileName,
                        tools
                    )
                } else null
            }
        }
    }

    override fun queryImage(imageBytes: ByteArray, query: String, modelName: String): Flow<String> {
        crashlytics.log("NoteAssistantRepositoryImpl: Query Image. Query: $query")
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

    override fun runAiAssistant(
        prompt: String,
        modelName: String,
        extraTools: List<ToolSet>,
    ): Flow<String> {
        crashlytics.log("NoteAssistantRepositoryImpl: Assistant. Prompt len: ${prompt.length}, model: $modelName")
        val composedTools = extraTools + stableTools
        return when (modelName) {
            "None", "Gemini 3 Flash (Cloud)", "Gemini Nano (System)" -> emptyFlow()
            else -> flow {
                val models = aiModelRepository.getModels().first()
                val selectedModel = models.find { it.name == modelName }
                if (selectedModel != null && selectedModel.isLlm) {
                    gemmaLocalDataSource.runAssistant(
                        prompt = prompt,
                        fileName = selectedModel.downloadFileName,
                        tools = composedTools,
                    ).collect { emit(it) }
                }
            }
        }
    }

    override suspend fun resetAiAssistantSession() {
        gemmaLocalDataSource.resetSession()
    }
 
    override suspend fun prepareAssistant(modelName: String, extraTools: List<ToolSet>) {
        val models = aiModelRepository.getModels().first()
        val selectedModel = models.find { it.name == modelName }
        if (selectedModel != null) {
            val composedTools = extraTools + stableTools
            gemmaLocalDataSource.prepareAssistant(
                fileName = selectedModel.downloadFileName,
                tools = composedTools,
            )
        }
    }
 
    override suspend fun stopAssistantInference() {
        gemmaLocalDataSource.stopInference()
    }
}