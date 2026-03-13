package com.mintanable.notethepad.feature_ai.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

class GeminiService(private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.1f
            topK = 5
            topP = 0.1f
        }
    )

    suspend fun generateTags(title: String, content: String, existingTags: List<String>): List<String> {
        val prompt = """
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

        return try {
            val response = model.generateContent(prompt)
            response.text?.split(",")?.map { it.trim().lowercase().replaceFirstChar { it.uppercase() } } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
