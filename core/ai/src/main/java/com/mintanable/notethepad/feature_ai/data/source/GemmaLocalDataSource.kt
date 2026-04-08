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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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

class GemmaLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val crashlytics = FirebaseCrashlytics.getInstance()

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
                crashlytics.log("$TAG: Init start ($fileName). Audio: $supportAudio, Image: $supportImage")
                closeInstanceInternal()

                val modelFile = File(context.getExternalFilesDir(null), fileName)
                val modelPath = modelFile.absolutePath

                try {
                    val runtime = Runtime.getRuntime()
                    val availableMem = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / 1024 / 1024
                    crashlytics.setCustomKey("pre_init_mem_mb", availableMem)

                    System.gc()
                    Runtime.getRuntime().gc()
                    kotlinx.coroutines.delay(if (supportImage || supportAudio) 500 else 200)

                    val mainBackend = if (supportImage || supportAudio) Backend.CPU() else Backend.GPU()
                    crashlytics.setCustomKey("active_backend", if (mainBackend is Backend.CPU) "CPU" else "GPU")

                    val engineConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = mainBackend,
                        visionBackend = if (supportImage) Backend.GPU() else null,
                        audioBackend = if (supportAudio) Backend.CPU() else null,
                        maxNumTokens = if (supportAudio) 512 else maxNumTokens,
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
                    crashlytics.log("$TAG: Init Failed - ${e.message}")
                    crashlytics.recordException(e)
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
        crashlytics.log("$TAG: Inference run. Prompt: ${prompt.take(50)}...")

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
                        crashlytics.log("$TAG: Native Inference Error")
                        crashlytics.recordException(throwable)
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
        crashlytics.log("$TAG: Internal close instance")
        cancelAndAwaitInference(inst.conversation)
        try { inst.conversation.close() } catch (_: Exception) { }
        try { inst.engine.close() } catch (_: Exception) { }
        activeInstance = null
    }

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
                runInference(prompt).collect { chunk -> textResponse += chunk }
            }
            textResponse.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Tag generation failed: ${e.message}")
            null
        } finally {
            cleanup()
        }
    }

    // ── Task: Transcribe Audio ────────────────────────────────────────────

    suspend fun prepareForAudio(selectedModel: AiModel) {
        initialize(
            fileName = selectedModel.downloadFileName,
            supportAudio = true,
            supportImage = false,
            samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2),
        )
    }

    suspend fun transcribeAudioFile(
        processedAudio: ByteArray?,
        onTranscription: (String) -> Unit,
    ) {
        try {
            withTimeout(60_000L) {
                runInference(prompt = "Transcribe this audio:", audioData = processedAudio).collect { chunk ->
                    onTranscription(chunk)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed: ${e.message}")
            crashlytics.log("$TAG: Transcription error")
        }
    }

    suspend fun resetConversation(systemInstruction: String? = null) {
        mutex.withLock {
            val instance = activeInstance ?: return
            try { instance.conversation.close() } catch (_: Exception) {}
            instance.conversation = instance.engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2),
                    systemInstruction = systemInstruction?.let { Contents.of(it) }
                )
            )
        }
    }

    // ── Task: Summarize Note (text-only) ──────────────────────────────────

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeNote(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        val maxChunkChars = 12_000 //roughly 3072 tokens
        try {
            if (prompt.length <= maxChunkChars) {
                return@withContext summarizeChunk(prompt, fileName)
            }
            val chunks = prompt.chunked(maxChunkChars)
            val partialSummaries = chunks.map { summarizeChunk("Summarize: $it", fileName) ?: "" }.filter { it.isNotBlank() }
            return@withContext summarizeChunk("Synthesize these summaries into a final 2-3 sentences overview:: ${partialSummaries.joinToString(" ")}", fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Summarization failed: ${e.message}")
            crashlytics.log("$TAG: Summarization failed errpr")
            null
        } finally {
            cleanup()
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeChunk(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initialize(
                fileName = fileName,
                supportAudio = false,
                supportImage = false,
                maxNumTokens = 4096,
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.2),
                systemInstruction = "Summarize this note in 2 sentences."
            )
            var summary = ""
            runInference(prompt).collect { chunk -> summary += chunk }
            summary.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Chunk Summarization failed: ${e.message}")
            null
        } finally {
            cleanup()
        }
    }

    // ── Task: Describe Image (short description) ──────────────────────────

    @OptIn(ExperimentalApi::class)
    suspend fun describeImage(imageBytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            initialize(
                fileName = fileName,
                supportAudio = false,
                supportImage = true,
                maxNumTokens = 256,
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.3),
                systemInstruction = "Describe images concisely in 10-20 words or convert to text if it contains text."
            )

                var response = ""
            withTimeout(30_000L) {
                runInference(
                    prompt = "Describe this image in 10-20 words",
                    imageData = imageBytes
                ).collect { chunk ->
                    response += chunk
                }
            }
            response.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Image description failed: ${e.message}")
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
                      * Generic/Unknown: "Describe this image"
                    Example output: Convert to text, Create checklist, Summarize document
                """.trimIndent()
            )

            var response = ""
            withTimeout(30_000L) {
                runInference(
                    prompt = "Image Analysis Task: Suggest 3 short action phrases based on the categories provided. Output:",
                    imageData = imageBytes
                ).collect { chunk ->
                    response += chunk
                }
            }
            response.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Image analysis failed: ${e.message}")
            null
        } finally {
            cleanup()
        }
    }

    // ── Task: Query Image (streaming) ─────────────────────────────────────

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