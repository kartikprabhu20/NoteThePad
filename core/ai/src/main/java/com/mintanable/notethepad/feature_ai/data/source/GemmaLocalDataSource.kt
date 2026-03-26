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
import javax.inject.Inject

private const val TAG = "GemmaLocalDataSource"

/**
 * Manages on-device Gemma inference via LiteRT.
 *
 * Each task follows a strict **Initialize → Inference → Cleanup** lifecycle,
 * No engine state is carried between tasks — this avoids GPU OOM crashes
 * that occurred when switching engine capabilities (e.g. text → vision).
 */
class GemmaLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private data class ActiveInstance(
        val engine: Engine,
        var conversation: Conversation
    )

    private var activeInstance: ActiveInstance? = null
    private val mutex = Mutex()

    private val activeInferenceDeferred =
        java.util.concurrent.atomic.AtomicReference<CompletableDeferred<Unit>?>(null)

    // ── Status ───────────────────────────────────────────────────────────

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

    // ── Initialize ───────────────────────────────────────────────────────

    @OptIn(ExperimentalApi::class)
    private suspend fun initialize(
        fileName: String,
        supportAudio: Boolean,
        supportImage: Boolean,
        maxNumTokens: Int = 1024,
        samplerConfig: SamplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0),
        systemInstruction: String? = null,
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                // Always start clean — close any leftover engine from a previous task.
                closeInstanceInternal()

                val modelFile = File(context.getExternalFilesDir(null), fileName)
                val modelPath = modelFile.absolutePath

                try {
                    // Reclaim native/GPU memory before allocating the new engine.
                    System.gc()
                    Runtime.getRuntime().gc()
                    kotlinx.coroutines.delay(if (supportImage || supportAudio) 500 else 200)

                    // Use CPU for the main text backend when vision or audio needs GPU/CPU
                    // resources. Running both the LLM and vision encoder on GPU simultaneously
                    // causes native OOM on devices with limited GPU memory (scudo allocation
                    // failures in nativeCreateEngine). Gallery can get away with dual-GPU
                    // because it initializes the engine once at screen open with minimal UI
                    // overhead; NoteThePad has heavier concurrent GPU usage from Compose
                    // (SharedTransitionLayout, zoomed images, animations).
                    val mainBackend = if (supportImage || supportAudio) Backend.CPU() else Backend.GPU()

                    val engineConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = mainBackend,
                        visionBackend = if (supportImage) Backend.GPU() else null,
                        audioBackend = if (supportAudio) Backend.CPU() else null,
                        maxNumTokens = maxNumTokens,
                        cacheDir = if (modelPath.startsWith("/data/local/tmp"))
                            context.getExternalFilesDir(null)?.absolutePath else null,
                    )

                    val engine = Engine(engineConfig)
                    engine.initialize()

                    val conversation = engine.createConversation(
                        ConversationConfig(
                            samplerConfig = samplerConfig,
                            systemInstruction = systemInstruction?.let { Contents.of(it) }
                        )
                    )

                    activeInstance = ActiveInstance(engine, conversation)
                    Log.d(TAG, "Engine initialized (audio=$supportAudio, image=$supportImage)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize: ${e.message}")
                    closeInstanceInternal()
                    throw e
                }
            }
        }
    }

    // ── Inference ─────────────────────────────────────────────────────────

    private fun runInference(
        prompt: String,
        audioData: ByteArray? = null,
        imageData: ByteArray? = null
    ): Flow<String> = flow {
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
                        Log.e(TAG, "Inference Error", throwable)
                        responseChannel.close(throwable)
                    }
                }
            }
        )

        for (text in responseChannel) {
            emit(text)
        }
    }.flowOn(Dispatchers.IO)

    // ── Cleanup ──────────────────────────────────────────────────────────

    private suspend fun cancelAndAwaitInference(conversation: Conversation) {
        val deferred = activeInferenceDeferred.get() ?: return
        if (deferred.isCompleted) return
        try {
            conversation.cancelProcess()
            withTimeout(3_000L) { deferred.await() }
        } catch (e: Exception) {
            Log.w(TAG, "cancelAndAwaitInference: ${e.message}")
        }
    }

    private suspend fun closeInstanceInternal() {
        val inst = activeInstance ?: return
        cancelAndAwaitInference(inst.conversation)
        try { inst.conversation.close() } catch (_: Exception) { }
        try { inst.engine.close() } catch (_: Exception) { }
        activeInstance = null
    }

    /**
     * Public cleanup — releases engine + conversation.
     * Safe to call from any thread. No-op if nothing is active.
     */
    suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                closeInstanceInternal()
            }
        }
        Log.d(TAG, "Cleanup done.")
    }

    // ── Task: Generate Tags (text-only) ──────────────────────────────────

    @OptIn(ExperimentalApi::class)
    suspend fun generateTags(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initialize(
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
            withTimeout(20_000L) {
                runInference(prompt).collect { chunk ->
                    textResponse += chunk
                }
            }
            textResponse.trim()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Tag inference timed out")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Tag generation failed: ${e.message}")
            null
        } finally {
            cleanup()
        }
    }

    // ── Task: Transcribe Audio ────────────────────────────────────────────

    suspend fun transcribeAudioFile(
        selectedModel: AiModel,
        processedAudio: ByteArray?,
        onTranscription: (String) -> Unit,
    ) {
        try {
            initialize(
                fileName = selectedModel.downloadFileName,
                supportAudio = true,
                supportImage = false,
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2),
            )

            withTimeout(60_000L) {
                runInference(
                    prompt = "Transcribe this audio:",
                    audioData = processedAudio
                ).collect { chunk ->
                    onTranscription(chunk)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out")
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed: ${e.message}")
        } finally {
            cleanup()
        }
    }

    // ── Task: Summarize Note (text-only) ──────────────────────────────────

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeNote(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initialize(
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
            Log.e(TAG, "Summarization failed: ${e.message}")
            null
        } finally {
            cleanup()
        }
    }

    // ── Task: Analyze Image ───────────────────────────────────────────────

    @OptIn(ExperimentalApi::class)
    suspend fun analyzeImage(imageBytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initialize(
                fileName = fileName,
                supportAudio = false,
                supportImage = true,
                maxNumTokens = 4096,
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.00),
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
            Log.e(TAG, "Image analysis timed out")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Image analysis failed: ${e.message}")
            null
        } finally {
            cleanup()
        }
    }

    // ── Task: Query Image (streaming) ─────────────────────────────────────

    /**
     * Initialize for image query and return a streaming Flow.
     * Caller MUST call [cleanup] when done collecting.
     */
    @OptIn(ExperimentalApi::class)
    suspend fun queryImage(imageBytes: ByteArray, query: String, fileName: String): Flow<String> {
        initialize(
            fileName = fileName,
            supportAudio = false,
            supportImage = true,
            maxNumTokens = 4096,
            samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.00),
        )
        return runInference(prompt = query, imageData = imageBytes)
    }
}
