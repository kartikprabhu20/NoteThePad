package com.mintanable.notethepad.feature_ai.data.source

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.CancellationException
import javax.inject.Inject

class GemmaLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private var currentModelPath: String? = null
    private val mutex = Mutex()

    private suspend fun getOrInitializeEngine(fileName: String): Engine = mutex.withLock {
        val modelFile = File(context.getExternalFilesDir(null), fileName)
        val newPath = modelFile.absolutePath

        if (engine == null || currentModelPath != newPath) {
            Log.d("kptest", "Initializing Gemma Engine with model: $fileName")
            engine?.close()

            val config = EngineConfig(
                modelPath = newPath,
                backend = Backend.GPU(), // Gemma 3 optimization
                cacheDir = context.cacheDir.path,
                visionBackend = Backend.GPU(),
                audioBackend = Backend.CPU(),
                maxNumTokens = 2048, // Increased for longer notes
            )

            engine = Engine(config).apply { initialize() }
            currentModelPath = newPath
        }
        return engine!!
    }

    suspend fun generateTags(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                withTimeout(20000L) { //20seconds
                    val activeEngine = getOrInitializeEngine(fileName)

                    //Configure the "One-Shot" Conversation
                    val conversationConfig = ConversationConfig(
                        systemInstruction = Contents.of(
                            text = """
                                You are an expert organizational assistant for the app "NoteThePad".
                                TASK: Analyze the note and suggest 3-5 relevant one-word tags.
        
                                CRITICAL RULES:
                                 1. Use EXACT names from 'Existing Tags' if they match semantically.
                                 2. Return ONLY a comma-separated list of words.
                                 3. No hashtags, no explanations, no conversational filler.
        
                                Example Output: Work, Finance, Urgent
                            """.trimIndent()
                        ),
                        samplerConfig = SamplerConfig(
                            temperature = 0.2,
                            topK = 40,
                            topP = 0.9,
                            seed = 101
                        )
                    )

                    // Inference
                    activeEngine.createConversation(conversationConfig)?.use { conversation ->
                        var textResponse = ""
                        conversation.sendMessageAsync(prompt)
                            .catch { Log.e("kptest", "Inference failed: ${it.message}") }
                            .collect {
                                textResponse = it.toString()
                            }
                        textResponse.trim()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("kptest", "Inference timed out after 20 seconds: $e")
                engine?.close()
                engine = null
                null
            } catch (e: Exception) {
                Log.e("kptest", "LiteRT-LM Error: ${e}")
                engine?.close()
                engine = null
                null
            }
        }
    }

    fun checkLocalStatus(selectedModel: AiModel?): Flow<AiModelDownloadStatus> = flow {
        if (selectedModel != null) {
            val file = File(context.getExternalFilesDir(null), selectedModel.downloadFileName)
            if (file.exists()) {
                emit(AiModelDownloadStatus.Ready)
            } else {
                emit(AiModelDownloadStatus.Downloadable)
            }
        } else {
            emit(AiModelDownloadStatus.Unavailable)
        }
    }

    suspend fun summarizeNote(content: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val activeEngine = getOrInitializeEngine(fileName)
            activeEngine.createConversation(
                ConversationConfig(systemInstruction = Contents.of(text = "Summarize this note in 2 sentences."))
            ).use { conversation ->
                var summary = ""
                conversation.sendMessageAsync(content).collect { summary += it.toString() }
                summary
            }
        } catch (e: Exception) {
            Log.e("kptest", "SummarizeNote error $e")
            engine?.close()
            engine = null
            null
        }
    }

    suspend fun transcribeAudioFile(audioFile: File, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val activeEngine = getOrInitializeEngine(fileName)

            //Prepare the audio input
            val pfd = ParcelFileDescriptor.open(audioFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val audioContent = Content.AudioFile(audioFile.absolutePath)

            //Create a one-shot conversation for transcription
            val config = ConversationConfig(
                systemInstruction = Contents.of(
                    text = "You are a high-accuracy speech-to-text engine. Transcribe the provided audio exactly as heard. Do not add commentary."
                )
            )

            activeEngine.createConversation(config).use { conversation ->
                conversation.sendMessageAsync(
                    Contents.of(audioContent),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            Log.i("kptest", "transcribeAudioFile: message= $message")
                        }

                        override fun onDone() {
                            Log.i("kptest", "transcribeAudioFile completed")
                        }

                        override fun onError(throwable: Throwable) {
                            if (throwable is CancellationException) {
                                Log.i("kptest", "The inference is cancelled.")
                            } else {
                                Log.e("kptest", "onError", throwable)
                            }
                        }
                    },
                )
            }
            ""
        } catch (e: Exception) {
            Log.e("kptest", "Gemma Audio Error: ${e.message}")
            null
        }
    }
}