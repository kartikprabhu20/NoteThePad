package com.mintanable.notethepad.feature_ai.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.mintanable.notethepad.core.common.ReminderScheduler
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class ReminderTools(
    private val onReminderRequested: (String, Long) -> Unit,
    private val noteRepository: NoteRepository? = null,
    private val reminderScheduler: ReminderScheduler? = null,
) : ToolSet {

    @Tool(description = "Returns current system time in universal milliseconds.")
    fun getCurrentTimeMs(): Double {
        return System.currentTimeMillis().toDouble()
    }

    @Tool(description = "Calculates the timestamp in milliseconds for a relative day/time. Use this for 'Next Sunday' or 'Tomorrow at 4pm'. Returns a Double you must pass to add_reminder.")
    fun getTimestampForInstruction(
        @ToolParam(description = "Days from today (0=today, 1=tomorrow, 7=next week)") dayOffset: Int,
        @ToolParam(description = "Hour in 24h format (0-23)") hour: Int,
        @ToolParam(description = "Minute (0-59)") minute: Int,
    ): Double {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis.toDouble()
    }

    @Tool(description = "Sets a reminder. The reminderMs parameter MUST be the Double returned by get_timestamp_for_instruction or get_current_time_ms — never a hand-written numeric literal, and never contain words or non-digit characters.")
    fun addReminder(
        @ToolParam(description = "The title or subject of the reminder") title: String,
        @ToolParam(description = "Target time as UTC milliseconds Double — use the value returned by get_timestamp_for_instruction") reminderMs: Double
    ): String {
        onReminderRequested(title, reminderMs.toLong())
        return "Reminder set successfully for $reminderMs"
    }

    @Tool(description = "Clears the reminder on an existing note by id.")
    fun clearReminder(
        @ToolParam(description = "Note id whose reminder should be removed") noteId: String
    ): String = runBlocking(Dispatchers.IO) {
        val repo = noteRepository ?: return@runBlocking "repository_unavailable"
        repo.updateReminderTime(noteId, -1L)
        reminderScheduler?.cancel(noteId.hashCode().toLong())
        "ok"
    }

    @Tool(description = "Lists upcoming reminders as a JSON array of {id, title, reminderMs}.")
    fun listUpcomingReminders(
        @ToolParam(description = "Max results (1-50)") limit: Int
    ): String = runBlocking(Dispatchers.IO) {
        val repo = noteRepository ?: return@runBlocking "[]"
        val capped = limit.coerceIn(1, 50)
        val notes = repo.getNotesWithFutureReminders(System.currentTimeMillis())
            .sortedBy { it.noteEntity.reminderTime }
            .take(capped)
        val arr = JSONArray()
        notes.forEach { nwt ->
            val n = nwt.noteEntity
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("reminderMs", n.reminderTime)
            })
        }
        arr.toString()
    }
}
