package com.mintanable.notethepad.feature_ai.data.source

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject

class GemmaLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private data class ActiveInstance(
        val engine: Engine,
        var conversation: Conversation
    )

    private var activeInstance: ActiveInstance? = null
    private val mutex = Mutex()
    private var currentModelPath: String? = null

    fun checkLocalStatus(selectedModel: AiModel?): Flow<AiModelDownloadStatus> = flow {
        if (selectedModel == null) {
            emit(AiModelDownloadStatus.Unavailable)
            return@flow
        }

        val file = File(context.getExternalFilesDir(null), selectedModel.downloadFileName)
        if (file.exists() && file.length() > 0) {
            emit(AiModelDownloadStatus.Ready)
        } else {
            emit(AiModelDownloadStatus.Downloadable)
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun initializeEngine(
        fileName: String,
        systemInstruction: String? = null,
        supportAudio: Boolean = true,
        supportImage: Boolean = false,
    ) = mutex.withLock {
        val modelFile = File(context.getExternalFilesDir(null), fileName)
        val newPath = modelFile.absolutePath

        if (activeInstance != null && currentModelPath == newPath) {
            activeInstance?.conversation?.close()
            activeInstance?.conversation =
                createNewConversation(activeInstance!!.engine, systemInstruction)
            return@withLock
        }

        //Clean up old engine before starting new one (Crucial for SIGSEGV prevention)
        closeActiveInstance()

        try {
            val engineConfig = EngineConfig(
                modelPath = newPath,
                backend = Backend.GPU(),
                visionBackend = if (supportImage) Backend.GPU() else null, // Only load the vision encoder when images are actually needed.

                audioBackend = if (supportAudio) Backend.CPU() else null, // Audio encoder must run on CPU for Gemma 3n.
                maxNumTokens = 1024,
                // Pass null for models stored in external files dir (normal production path).
                // Passing context.cacheDir.path for every model forces LiteRT to write KV
                // cache to internal storage which has tighter space limits.
                cacheDir = if (newPath.startsWith("/data/local/tmp"))
                    context.getExternalFilesDir(null)?.absolutePath else null,
            )

            val engine = Engine(engineConfig)
            engine.initialize()

            val conversation = createNewConversation(engine, systemInstruction)

            activeInstance = ActiveInstance(engine, conversation)
            currentModelPath = newPath
            Log.d("kptest", "Gemma Engine Initialized Successfully")
        } catch (e: Exception) {
            Log.e("kptest", "Failed to initialize: ${e.message}")
            closeActiveInstance()
            throw e
        }
    }

    private fun createNewConversation(engine: Engine, systemInstruction: String?): Conversation {
        return engine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 40,
                    topP = 0.9,
                    temperature = 0.2
                ),
                systemInstruction = systemInstruction?.let { Contents.of(it) }
            )
        )
    }

    @OptIn(ExperimentalApi::class)
    suspend fun generateTags(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            //Ensure the engine is warm with the specific "Tagging" system instruction
            initializeEngine(
                fileName = fileName,
                supportAudio = false,
                supportImage = false,
                systemInstruction = """
                You are an expert organizational assistant for the app "NoteThePad".
                TASK: Analyze the note and suggest 3-5 relevant one-word tags.
                CRITICAL RULES:
                 1. Use EXACT names from 'Existing Tags' if they match semantically.
                 2. Return ONLY a comma-separated list of words.
                 3. No hashtags, no explanations, no conversational filler.
                Example Output: Work, Finance, Urgent
            """.trimIndent()
            )

            // Use the managed runInference pipe
            var textResponse = ""
            withTimeout(20000L) {
                runInference(prompt).collect { chunk ->
                    textResponse += chunk
                }
            }
            textResponse.trim()
        } catch (e: TimeoutCancellationException) {
            Log.e("kptest", "Tag inference timed out")
            null
        } catch (e: Exception) {
            Log.e("kptest", "Tag generation failed: ${e.message}")
            null
        }
    }

    suspend fun transcribeAudioFile(selectedModel: AiModel, processedAudio: ByteArray?, onTranscription: (String) -> Unit) {
        initializeEngine(
            fileName = selectedModel.downloadFileName,
            supportAudio = true,
            supportImage = false
        )

        // Run inference using the ByteArray
        runInference(
            prompt = "Transcribe this audio:",
            audioData = processedAudio
        ).collect { chunk ->
            onTranscription(chunk)
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeNote(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initializeEngine(
                fileName = fileName,
                supportAudio = false,
                supportImage = false,
                systemInstruction = "Summarize this note in 2 sentences."
            )

            var summary = ""
            runInference(prompt).collect { chunk ->
                summary += chunk
            }
            summary.trim()
        } catch (e: Exception) {
            Log.e("kptest", "Summarization failed: ${e.message}")
            null
        }
    }

    fun runInference(prompt: String, audioData: ByteArray? = null): Flow<String> = flow {
        val instance = activeInstance ?: throw IllegalStateException("Engine not initialized")

        val contents = mutableListOf<Content>()

        // Pass the raw bytes directly to the native multimodal encoder
        audioData?.let {
            contents.add(Content.AudioBytes(it))
        }

        contents.add(Content.Text(prompt))

        val responseChannel = Channel<String>(Channel.UNLIMITED)

        instance.conversation.sendMessageAsync(
            Contents.of(contents),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    Log.e("kptest", "runInference: message: ${message.toString()}")

                    responseChannel.trySend(message.toString())
                }
                override fun onDone() {
                    Log.e("kptest", "runInference: onDone")
                    responseChannel.close()
                }
                override fun onError(throwable: Throwable) {
                    // This captures the native SIGSEGV or JNI errors
                    Log.e("kptest", "Inference Error", throwable)
                    responseChannel.close(throwable)
                }
            }
        )

        for (text in responseChannel) {
            emit(text)
        }
    }.flowOn(Dispatchers.IO)

    fun closeActiveInstance() {
        activeInstance?.apply {
            try {
                conversation.close()
            } catch (e: Exception) {
            }
            try {
                engine.close()
            } catch (e: Exception) {
            }
        }
        activeInstance = null
        currentModelPath = null
    }
}