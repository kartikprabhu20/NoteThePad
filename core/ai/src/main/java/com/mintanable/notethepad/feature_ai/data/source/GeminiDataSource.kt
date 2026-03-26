package com.mintanable.notethepad.feature_ai.data.source

import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiDataSource(private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.1f
            topK = 5
            topP = 0.1f
        }
    )

    private val visionModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.9f
        }
    )

    suspend fun analyzeImage(imageBytes: ByteArray): List<String> {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return emptyList()
            val prompt = """
                Analyze this image and suggest exactly 3 short action phrases the user might want to do with it.
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

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = visionModel.generateContent(inputContent)
            response.text?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(3) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun queryImage(imageBytes: ByteArray, query: String): Flow<String> = flow {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return@flow
        val inputContent = content {
            image(bitmap)
            text(query)
        }
        val response = visionModel.generateContentStream(inputContent)
        response.collect { chunk ->
            chunk.text?.let { emit(it) }
        }
    }

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
