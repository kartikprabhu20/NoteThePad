package com.mintanable.notethepad.feature_ai.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

class ReminderTools(
    private val onReminderRequested: (String, Long) -> Unit
) : ToolSet {

    @Tool(description = "Returns current system time in universal milliseconds.")
    fun getCurrentTimeMs(): Double {
        return System.currentTimeMillis().toDouble()
    }

    @Tool(description = "Calculates the timestamp for a specific day and time. Use this for 'Next Sunday' or 'Tomorrow'.")
    fun getTimestampForInstruction(dayOffset: Int, hour: Int, minute: Int): Double {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        return calendar.timeInMillis.toDouble()
    }

    @Tool(description = "Sets a reminder for a specific date and time.")
    fun addReminder(
        @ToolParam(description = "The title or subject of the reminder") title: String,
        @ToolParam(description = "The target time in UTC milliseconds") reminderMs: Double
    ): String {
        onReminderRequested(title, reminderMs.toLong())
        return "Reminder set successfully for $reminderMs"
    }
}