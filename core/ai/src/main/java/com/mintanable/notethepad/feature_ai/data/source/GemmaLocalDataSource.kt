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
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
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
): GenericDataSource() {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private data class ActiveInstance(
        val engine: Engine,
        var conversation: Conversation,
        val fileName: String,
        val supportAudio: Boolean,
        val supportImage: Boolean
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
        tools: List<ToolProvider> = listOf()
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = activeInstance
                if (current != null && current.fileName == fileName &&
                    current.supportAudio == supportAudio && current.supportImage == supportImage) {
                    crashlytics.log("$TAG: Reusing existing instance ($fileName)")
                    return@withLock
                }

                crashlytics.log("$TAG: Init start ($fileName). Audio: $supportAudio, Image: $supportImage")
                closeInstanceInternal()

                val modelFile = File(context.getExternalFilesDir(null), fileName)
                val modelPath = modelFile.absolutePath

                try {
                    val runtime = Runtime.getRuntime()
                    val availableMem =(runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / 1024 / 1024
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
                            systemInstruction = systemInstruction?.let { Contents.of(it) },
                            tools = tools
                        )
                    )

                    activeInstance = ActiveInstance(
                        engine = engine,
                        conversation = conversation,
                        fileName = fileName,
                        supportAudio = supportAudio,
                        supportImage = supportImage
                    )
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

    suspend fun stopInference() {
        mutex.withLock {
            val inst = activeInstance ?: return
            cancelAndAwaitInference(inst.conversation)
        }
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
                """.trimIndent(),
                maxNumTokens = 4096
            )
            var textResponse = ""
            withTimeout(20_000L) {
                runInference(prompt.take(12000)).collect { chunk -> textResponse += chunk }
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

    suspend fun resetSession() {
        mutex.withLock {
            val instance = activeInstance ?: return
            crashlytics.log("$TAG: Resetting session conversation")
            try { instance.conversation.close() } catch (_: Exception) {}
            instance.conversation = instance.engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.4),
                )
            )
        }
    }

    // ── Task: Summarize Note (text-only) ──────────────────────────────────

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeNote(prompt: String, fileName: String, tools: List<ToolSet>): String? = withContext(Dispatchers.IO) {
        val maxChunkChars = 12_000 //roughly 3072 tokens
        try {
            if (prompt.length <= maxChunkChars) {
                return@withContext summarizeChunk(prompt, fileName, tools)
            }
            val chunks = prompt.chunked(maxChunkChars)
            val partialSummaries = chunks.map { summarizeChunk("Summarize: $it", fileName, tools) ?: "" }.filter { it.isNotBlank() }
            return@withContext summarizeChunk("Synthesize these summaries into a final 2-3 sentences overview:: ${partialSummaries.joinToString(" ")}", fileName, tools)
        } catch (e: Exception) {
            Log.e(TAG, "Summarization failed: ${e.message}")
            crashlytics.log("$TAG: Summarization failed errpr")
            null
        } finally {
            cleanup()
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeChunk(prompt: String, fileName: String, tools: List<ToolSet>): String? =
        withContext(Dispatchers.IO) {
            try {
                initialize(
                    fileName = fileName,
                    supportAudio = false,
                    supportImage = false,
                    maxNumTokens = 4096,
                    samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.2),
                    systemInstruction = ASSISTANT_SYSTEM_PROMPT_SUMMARIZE,
                    tools = tools.map { tool(it) }
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
                maxNumTokens = 512,
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

    // ── Task: General Assistant (streaming, tool-aware) ──────────────────

    @OptIn(ExperimentalApi::class)
    fun runAssistant(
        prompt: String,
        fileName: String,
        tools: List<ToolSet>,
    ): Flow<String> = flow {
        try {
            initialize(
                fileName = fileName,
                supportAudio = false,
                supportImage = false,
                maxNumTokens = 4096,
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.4),
                systemInstruction = ASSISTANT_SYSTEM_PROMPT_GENERAL,
                tools = tools.map { tool(it) },
            )
            runInference(prompt).collect { emit(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Assistant run failed: ${e.message}")
            Log.e(TAG, "Offending prompt: $prompt")
            crashlytics.log("$TAG: Assistant run failed prompt=$prompt")
        }
    }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalApi::class)
    suspend fun prepareAssistant(fileName: String) {
        initialize(
            fileName = fileName,
            supportAudio = false,
            supportImage = false,
            maxNumTokens = 4096,
            samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.4),
            systemInstruction = ASSISTANT_SYSTEM_PROMPT_GENERAL,
        )
    }
}

private val TOOL_CATALOG = """
    You have access to tools grouped by category:
    - REMINDERS: get_current_time_ms, get_system_date_time, get_timestamp_for_instruction, add_reminder, clear_reminder, list_upcoming_reminders
    - NOTE CRUD: create_note, update_note, delete_note, get_note, list_recent_notes
    - SEARCH: search_notes_by_text, search_notes_by_tag, list_notes_with_reminders, list_notes_by_color
    - TAGS: list_all_tags, create_tag, add_tag_to_note, remove_tag_from_note, suggest_tags
    - MEDIA: list_attachments, transcribe_audio_attachment, describe_image_attachment
    - BACKUP: schedule_backup, cancel_scheduled_backup, trigger_backup_now, restore_from_backup, is_user_signed_in_for_backup

    INTENT → TOOL hints (use these before saying a tool doesn't exist):
    - "yellow notes" / "red notes" / "blue notes" → list_notes_by_color (pass a color NAME like "yellow", not a number)
    - "notes mentioning <word>" / "find <word>" → search_notes_by_text
    - "notes tagged X" → search_notes_by_tag
    - "am I signed in" / "Drive backup status" → is_user_signed_in_for_backup
    - "back up now" → trigger_backup_now
    - "restore my notes" → restore_from_backup
    - "remind me <when>" / "set a reminder" → get_timestamp_for_instruction THEN add_reminder
    - "what's scheduled" / "upcoming reminders" → list_upcoming_reminders
    - "tag this note X" / "add tag X" → add_tag_to_note (it AUTO-CREATES the tag; do NOT call create_tag first)
    - "summarize my notes" → search_notes_by_tag / list_recent_notes THEN get_note for each id THEN synthesize

    Tool selection rules:
    1. Prefer specific tools over general ones (e.g. search_notes_by_tag over list_recent_notes when filtering).
    2. For reminders: ALWAYS call get_system_date_time to know the current date, day and time, and then get_timestamp_for_instruction for time in milliseconds, then pass its Double result to add_reminder. Never type a number literal for reminder_ms.
    3. Never fabricate note ids — look them up with search or list tools first.
    4. Tools return JSON strings or plain status codes; parse them before acting.
    5. For list_notes_by_color, the color parameter is a NAME ("yellow", "redOrange"), not an integer.
""".trimIndent()

private val ASSISTANT_SYSTEM_PROMPT_SUMMARIZE = """
    You are a Note Assistant for NoteThePad.

    $TOOL_CATALOG

    CRITICAL INSTRUCTIONS:
    1. If the note mentions a task, date, or "tomorrow/next week", you MUST call add_reminder.
    2. Do NOT just write "I set a reminder" in the text. You MUST execute the add_reminder tool.
    3. To set a reminder:
        a. First, call get_system_date_time to know the current day of the week and date.
        b. Based on that, calculate the 'dayOffset' (e.g., if today is Tuesday and the user says 'Friday', offset is 3).
        c. Call get_timestamp_for_instruction with the calculated offset.
        d. Finally, call add_reminder with that result.
    4. Summarize the note content after calling your tools.
""".trimIndent()

private val ASSISTANT_SYSTEM_PROMPT_GENERAL = """
    You are the NoteThePad AI assistant. You help the user manage notes, tags, reminders, attachments, and backups via tools.

    $TOOL_CATALOG

    Reminder workflow (CRITICAL):
    - To set a reminder for e.g. "tomorrow at 4pm":
        1. Call get_timestamp_for_instruction(dayOffset=1, hour=16, minute=0) — this returns a Double.
        2. Call add_reminder(title="<short title>", reminderMs=<the Double from step 1>).
    - NEVER compute reminderMs inline. NEVER type a raw number or any non-digit character into reminderMs.
    - The reminderMs argument must be copied verbatim from the previous tool's return value.

    Tagging workflow:
    - To tag a note: call add_tag_to_note(noteId, tagName) ONCE. It auto-creates the tag if it doesn't exist. Do NOT call create_tag first.
    - If add_tag_to_note returns "add_failed", tell the user it couldn't be attached — do not silently retry more than once.

    Date/Time Intelligence (CRITICAL):
    - For questions about days, dates, or time durations (e.g. "How many days until Sunday?"), ALWAYS call get_system_date_time first to know today's context.
    - Calculate the answer based on the tool's return value (e.g. if today is Wednesday (3) and user asks for Sunday (7), the result is 4 days).
    - If calculating a future timestamp for a reminder, use get_system_date_time to find the day offset, then get_timestamp_for_instruction.

    Multi-step workflow:
    1. For multi-note queries (e.g. "summarize my work notes"): call a search/list tool, then get_note for each id you need to read, then produce your final answer.
    2. After ALL tool calls complete, ALWAYS produce a final natural-language reply that directly answers the user. Never stop after a tool call without replying.
    3. If a tool returns empty [] or "not_found", say so plainly in your final reply.
    4. Before saying "I cannot do that" or "there is no tool", re-scan the INTENT → TOOL hints above — the tool you need is almost always listed.

    Response rules:
    - Use tools to act on the user's data. Do not describe actions you could take — execute them.
    - For questions about the user's notes, always query them via search or list tools before answering.
    - Reply concisely in natural language after tool calls complete. Do not dump raw JSON to the user.
    - If a tool returns an error status (e.g. 'note_not_found'), recover or tell the user plainly.
""".trimIndent()