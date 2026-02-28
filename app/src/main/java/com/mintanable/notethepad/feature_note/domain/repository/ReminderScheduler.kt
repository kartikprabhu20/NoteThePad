package com.mintanable.notethepad.feature_note.domain.repository

interface ReminderScheduler {
    fun schedule(id: Long, title: String, content: String, reminderTime: Long)
    fun cancel(id: Long)
    fun canScheduleExactAlarms(): Boolean
}