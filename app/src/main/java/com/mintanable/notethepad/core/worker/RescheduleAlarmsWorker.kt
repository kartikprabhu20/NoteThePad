package com.mintanable.notethepad.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RescheduleAlarmsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: NoteRepository,
    private val scheduler: ReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val notesToRemind = repository.getNotesWithFutureReminders(currentTime)
        notesToRemind.forEach { note ->
            note.id?.let { scheduler.schedule(id = it, note.title, note.content, note.reminderTime) }
        }

        return Result.success()
    }
}