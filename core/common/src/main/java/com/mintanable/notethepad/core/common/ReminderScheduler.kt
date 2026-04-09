package com.mintanable.notethepad.core.common

interface ReminderScheduler {
    fun schedule(id: Long, title: String, content: String, reminderTime: Long)
    fun cancel(id: Long)
    fun canScheduleExactAlarms(): Boolean
}
