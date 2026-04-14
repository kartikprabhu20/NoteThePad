package com.mintanable.notethepad.feature_ai.data

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mintanable.notethepad.core.common.utils.readAndProcessImage
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.core.common.ReminderScheduler
import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import com.mintanable.notethepad.feature_ai.tools.ReminderTools
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "SummarizeNoteWorker"

@HiltWorker
class SummarizeNoteWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val noteRepository: NoteRepository,
    private val assistantRepository: NoteAssistantRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val noteId = inputData.getString("note_id")
        if (noteId.isNullOrBlank()) {
            Log.e(TAG, "Missing note_id in input data")
            return@withContext Result.failure()
        }

        Log.d(TAG, "Starting summarization for note: $noteId")

        try {
            // Fetch note from DB
            val noteWithTags = noteRepository.getNoteById(noteId)
            if (noteWithTags == null) {
                Log.e(TAG, "Note not found: $noteId")
                return@withContext Result.failure()
            }
            val note = noteWithTags.noteEntity
            Log.d(TAG, "Fetched note: title='${note.title}', images=${note.imageUris.size}, audios=${note.audioUris.size}")

            // Get model name from settings
            val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
            if (modelName == "None") {
                Log.w(TAG, "AI model is set to None, cannot summarize")
                return@withContext Result.failure()
            }
            Log.d(TAG, "Using AI model: $modelName")

            // Gather audio transcriptions (reuse existing, transcribe missing)
            val transcriptionMap: MutableMap<String, String> = mutableMapOf()
            runCatching {
                val json = JSONObject(note.audioTranscriptions)
                json.keys().forEach { key -> transcriptionMap[key] = json.getString(key) }
            }

            for (audioUri in note.audioUris) {
                if (transcriptionMap[audioUri].isNullOrBlank()) {
                    Log.d(TAG, "Transcribing audio: $audioUri")
                    try {
                        val transcription = StringBuilder()
                        assistantRepository.transcribeAudioFile(audioUri, modelName) { chunk ->
                            transcription.append(chunk)
                        }
                        if (transcription.isNotEmpty()) {
                            transcriptionMap[audioUri] = transcription.toString()
                            Log.d(TAG, "Transcription complete for: $audioUri (${transcription.length} chars)")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Audio transcription failed for $audioUri, skipping: ${e.message}")
                    }
                } else {
                    Log.d(TAG, "Reusing existing transcription for: $audioUri")
                }
            }

            // Describe images (in-memory only, not saved to DB)
            val imageDescriptions = mutableListOf<String>()
            for (imageUri in note.imageUris) {
                Log.d(TAG, "Describing image: $imageUri")
                try {
                    val imageBytes = readAndProcessImage(appContext, imageUri.toUri(), maxDimension = 1024)
                    if (imageBytes != null) {
                        val description = assistantRepository.describeImage(imageBytes, modelName)
                        if (!description.isNullOrBlank()) {
                            imageDescriptions.add(description)
                            Log.d(TAG, "Image description: $description")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Image description failed for $imageUri, skipping: ${e.message}")
                }
            }

            // Build prompt
            val allTranscriptions = transcriptionMap.values.filter { it.isNotBlank() }.joinToString("\n")
            val allImageDescriptions = imageDescriptions.joinToString(", ")
            val existingSummary = note.summary

            val prompt = buildString {
                appendLine("Summarize this note concisely in 5-6 sentences maximum.")
                appendLine()
                val now = java.time.LocalDateTime.now()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm")
                appendLine("Current date and time: ${now.format(formatter)}")
                appendLine()
                appendLine("Title: ${note.title}")
                appendLine("Content: ${note.content}")
                if (allImageDescriptions.isNotBlank()) {
                    appendLine("Image Descriptions: $allImageDescriptions")
                }
                if (allTranscriptions.isNotBlank()) {
                    appendLine("Audio Transcriptions: $allTranscriptions")
                }
                if (existingSummary.isNotBlank()) {
                    appendLine("Previous Summary (refine this): $existingSummary")
                }
            }
            Log.d(TAG, "Prompt built (${prompt.length} chars)")

            var pendingReminder: Pair<String, Long>? = null
            val tools = listOf(
                ReminderTools(
                    onReminderRequested = { title, time ->
                        Log.d(TAG, "Reminder requested: '$title' at $time")
                        pendingReminder = title to time
                    },
                    noteRepository = noteRepository,
                    reminderScheduler = reminderScheduler,
                )
            )

            // Generate summary
            val summary = assistantRepository.summarizeNote(prompt, modelName, tools)
            if (summary.isNullOrBlank()) {
                Log.e(TAG, "Summarization returned empty result")
                return@withContext Result.failure()
            }
            Log.d(TAG, "Summary generated: $summary")

            // Save summary to DB
            noteRepository.updateSummary(noteId, summary)
            Log.d(TAG, "Summary saved to DB for note: $noteId")

            // Schedule reminder if the LLM requested one
            pendingReminder?.let { (title, reminderTimeMs) ->
                if (reminderTimeMs > System.currentTimeMillis()) {
                    noteRepository.updateReminderTime(noteId, reminderTimeMs)
                    reminderScheduler.schedule(
                        id = noteId.hashCode().toLong(),
                        title = title,
                        content = RichTextSerializer.deserialize(note.content).rawText.take(200),
                        reminderTime = reminderTimeMs
                    )
                    Log.d(TAG, "Reminder scheduled: '$title' at $reminderTimeMs")
                } else {
                    Log.w(TAG, "Ignoring past reminder: $reminderTimeMs")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Summarization failed: ${e.message}", e)
            Result.failure()
        }
    }
}
