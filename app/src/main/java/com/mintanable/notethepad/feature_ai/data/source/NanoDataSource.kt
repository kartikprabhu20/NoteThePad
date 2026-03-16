package com.mintanable.notethepad.feature_ai.data.source

import com.google.mlkit.genai.prompt.Generation
import javax.inject.Inject

class NanoDataSource @Inject constructor() {
    private val client = Generation.getClient()

    suspend fun getStatus() = client.checkStatus()

    suspend fun generate(prompt: String): String? {
        return try {
            client.generateContent(prompt).candidates.firstOrNull()?.text
        } catch (e: Exception) {
            null
        }
    }
}