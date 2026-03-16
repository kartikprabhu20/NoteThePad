package com.mintanable.notethepad.feature_ai.data.source

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class GemmaLocalDataSource @Inject constructor(@ApplicationContext private val context: Context) {
    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null

    fun generate(prompt: String, fileName: String): String? {
        // Initialize the Engine (Static Config)
        if (llmInference == null) {
            val modelPath = File(context.getExternalFilesDir(null), fileName).absolutePath
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }

        // Initialize the Session (Dynamic Config / Temperature)
        if (session == null) {
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(0.2f)
                .setTopK(40)
                .setRandomSeed(101)
                .build()
            session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
        }

        // Inference
        // Use addQueryChunk for the prompt and generateResponse to get the result
        session?.addQueryChunk(prompt)
        return session?.generateResponse()
    }
}