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
import kotlinx.coroutines.CompletableDeferred
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
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicReference
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
    private var currentSupportAudio: Boolean = false
    private var currentSupportImage: Boolean = false

    private val activeInferenceDeferred = AtomicReference<CompletableDeferred<Unit>?>(null)

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
        samplerConfig: SamplerConfig = SamplerConfig(
            topK = 64,
            topP = 0.95,
            temperature = 1.0,
        ),
    ) {
       withContext(Dispatchers.IO) {
            mutex.withLock {
                val modelFile = File(context.getExternalFilesDir(null), fileName)
                val newPath = modelFile.absolutePath

              val capabilitiesMatch = currentSupportAudio == supportAudio && currentSupportImage == supportImage
                if (activeInstance != null && currentModelPath == newPath && capabilitiesMatch) {
                   cancelAndAwaitInference(activeInstance!!.conversation)
                    activeInstance?.conversation?.close()
                    activeInstance?.conversation =
                        createNewConversation(activeInstance!!.engine, systemInstruction, samplerConfig)
                    return@withLock
                }

                // Close existing engine (must cancel inference first).
                closeInstanceInternal()

                try {
                    val engineConfig = EngineConfig(
                        modelPath = newPath,
                        backend = Backend.GPU(),
                        visionBackend = if (supportImage) Backend.GPU() else null, // Only load the vision encoder when images are actually needed.
                        audioBackend = if (supportAudio) Backend.CPU() else null, // Audio encoder must run on CPU for Gemma 3n.
                        maxNumTokens = 1024,
                        // Pass null for models stored in external files dir (normal production
                        // path). Passing context.cacheDir.path forces LiteRT to write KV cache
                        // to internal storage which has tighter space limits.
                        cacheDir = if (newPath.startsWith("/data/local/tmp"))
                            context.getExternalFilesDir(null)?.absolutePath else null,
                    )

                    val engine = Engine(engineConfig)
                    engine.initialize()

                    val conversation = createNewConversation(engine, systemInstruction, samplerConfig)

                    activeInstance = ActiveInstance(engine, conversation)
                    currentModelPath = newPath
                    currentSupportAudio = supportAudio
                    currentSupportImage = supportImage
                    Log.d("kptest", "Gemma Engine Initialized (audio=$supportAudio, image=$supportImage)")
                } catch (e: Exception) {
                    Log.e("kptest", "Failed to initialize: ${e.message}")
                    closeInstanceInternal()
                    throw e
                }
            }
        }
    }

    private fun createNewConversation(
        engine: Engine,
        systemInstruction: String?,
        samplerConfig: SamplerConfig,
    ): Conversation {
        return engine.createConversation(
            ConversationConfig(
                samplerConfig = samplerConfig,
                systemInstruction = systemInstruction?.let { Contents.of(it) }
            )
        )
    }

    private suspend fun cancelAndAwaitInference(conversation: Conversation) {
        val deferred = activeInferenceDeferred.get() ?: return
        if (deferred.isCompleted) return
        try {
            conversation.cancelProcess()
            // Wait at most 3 s for the native thread to acknowledge cancellation.
            withTimeout(3_000L) { deferred.await() }
        } catch (e: Exception) {
            Log.w("kptest", "cancelAndAwaitInference: ${e.message}")
        }
    }

    /**
     * Internal close — called from within the mutex lock (by initializeEngine).
     * Must NOT try to acquire the mutex itself.
     */
    private suspend fun closeInstanceInternal() {
        val inst = activeInstance ?: return
        cancelAndAwaitInference(inst.conversation)
        try { inst.conversation.close() } catch (e: Exception) { }
        try { inst.engine.close() } catch (e: Exception) { }
        activeInstance = null
        currentModelPath = null
        currentSupportAudio = false
        currentSupportImage = false
    }

    /**
     * Public close — must be called when the owning ViewModel/repository is torn down.
     * Dispatches to IO so it is safe to call from any thread (including Main).
     */
    suspend fun closeActiveInstance() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                closeInstanceInternal()
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun generateTags(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initializeEngine(
                fileName = fileName,
                supportAudio = false,
                supportImage = false,
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.2),
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

    suspend fun transcribeAudioFile(
        selectedModel: AiModel,
        processedAudio: ByteArray?,
        onTranscription: (String) -> Unit,
    ) {
        initializeEngine(
            fileName = selectedModel.downloadFileName,
            systemInstruction = null,
            supportAudio = true,
            supportImage = false,
            samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2),
        )

        try {
            withTimeout(60_000L) {
                runInference(
                    prompt = "Transcribe this audio:",
                    audioData = processedAudio
                ).collect { chunk ->
                    onTranscription(chunk)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("kptest", "Audio transcription timed out")
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeNote(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initializeEngine(
                fileName = fileName,
                supportAudio = false,
                supportImage = false,
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.2),
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

    @OptIn(ExperimentalApi::class)
    suspend fun analyzeImage(imageBytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initializeEngine(
                fileName = fileName,
                supportAudio = false,
                supportImage = true,
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.7),
                systemInstruction = """
                    Analyze images and suggest exactly 3 short action phrases the user might want to do with it.
                    Rules:
                    - Each suggestion must be 2-5 words
                    - Return ONLY a comma-separated list of exactly 3 items
                    - Match these categories when applicable:
                      * Text/handwriting: "Convert to text"
                      * Food/recipe: "Give me recipe"
                      * Recognizable place: "Plan trip to this place"
                      * Actionable items: "Create checklist"
                      * Document/form: "Summarize document"
                      * Code/technical: "Explain this code"
                      * Nature/animal: "Identify this"
                    Example output: Convert to text, Create checklist, Summarize document
                """.trimIndent()
            )

            var response = ""
            withTimeout(30_000L) {
                runInference(
                    prompt = "Analyze this image and suggest exactly 3 short action phrases:",
                    imageData = imageBytes
                ).collect { chunk ->
                    response += chunk
                }
            }
            response.trim()
        } catch (e: TimeoutCancellationException) {
            Log.e("kptest", "Image analysis timed out")
            null
        } catch (e: Exception) {
            Log.e("kptest", "Image analysis failed: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun queryImage(imageBytes: ByteArray, query: String, fileName: String): Flow<String> {
        initializeEngine(
            fileName = fileName,
            supportAudio = false,
            supportImage = true,
            samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.7),
        )
        return runInference(prompt = query, imageData = imageBytes)
    }

    fun runInference(prompt: String, audioData: ByteArray? = null, imageData: ByteArray? = null): Flow<String> = flow {
        val instance = activeInstance ?: throw IllegalStateException("Engine not initialized")

        val contents = mutableListOf<Content>()
        audioData?.let { contents.add(Content.AudioBytes(it)) }
        imageData?.let { contents.add(Content.ImageBytes(it)) }
        contents.add(Content.Text(prompt))

        val responseChannel = Channel<String>(Channel.UNLIMITED)
        val deferred = CompletableDeferred<Unit>()
        activeInferenceDeferred.set(deferred)

        instance.conversation.sendMessageAsync(
            Contents.of(contents),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    val text = message.toString()
                    // Filter Gemma control tokens (e.g. "<ctrl99>").
                    // The gallery's ViewModel does the same — these appear as onMessage
                    // callbacks before onDone() and must not be forwarded to the caller.
                    if (!text.startsWith("<ctrl")) {
                        responseChannel.trySend(text)
                    }
                }

                override fun onDone() {
                    deferred.complete(Unit)
                    responseChannel.close()
                }

                override fun onError(throwable: Throwable) {
                    deferred.complete(Unit)
                    if (throwable is CancellationException) {
                        responseChannel.close()
                    } else {
                        Log.e("kptest", "Inference Error", throwable)
                        responseChannel.close(throwable)
                    }
                }
            }
        )

        for (text in responseChannel) {
            emit(text)
        }
    }.flowOn(Dispatchers.IO)
}
