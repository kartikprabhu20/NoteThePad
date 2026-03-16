package com.mintanable.notethepad.feature_ai.data.source

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject

class GemmaLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private var currentModelPath: String? = null
    private val mutex = Mutex()

    suspend fun generate(prompt: String, fileName: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                withTimeout(20000L) { //20seconds
                    val modelFile = File(context.getExternalFilesDir(null), fileName)
                    val newPath = modelFile.absolutePath

                    // Initialize Engine
                    if (engine == null || currentModelPath != newPath) {
                        engine?.close()

                        val config = EngineConfig(
                            modelPath = newPath,
                            backend = Backend.GPU(), // Mandatory for Gemma 3 speed
                            cacheDir = context.cacheDir.path
                        )

                        engine = Engine(config).apply { initialize() }
                        currentModelPath = newPath
                    }

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
                    engine?.createConversation(conversationConfig)?.use { conversation ->
                        try {
                            val responseMessage: Message = conversation.sendMessage(prompt)
                            val textResponse = responseMessage.contents.toString()

                            Log.d("kptest", "AI Raw Result: $textResponse")
                            textResponse
                        } catch (e: Exception) {
                            Log.e("kptest", "Inference failed: ${e.message}")
                            null
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("kptest", "Inference timed out after 20 seconds")
                ""
            } catch (e: Exception) {
                Log.e("kptest", "LiteRT-LM Error: ${e.message}")
                engine?.close()
                engine = null
                null
            }
        }
    }
}
