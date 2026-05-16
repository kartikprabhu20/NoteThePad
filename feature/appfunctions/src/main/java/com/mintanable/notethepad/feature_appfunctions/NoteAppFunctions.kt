package com.mintanable.notethepad.feature_appfunctions

import androidx.appfunctions.AppFunctionContext
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.feature_appfunctions.di.AppFunctionsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Implementation of the AppFunctions surface for NoteThePad.
 *
 * This class is intentionally NOT annotated with `@AppFunction`. It exists in
 * the feature module so the dependency on `NoteRepository` stays out of the
 * `:app` module. The thin bridge class in `:app/.../AppNoteAppFunctions.kt`
 * carries the `@AppFunction` annotations and delegates to this class so the
 * KSP aggregator in `:app` sees them.
 */
class NoteAppFunctions {

    suspend fun listNotes(appFunctionContext: AppFunctionContext): List<Note> {
        val repository = repository(appFunctionContext)
        val notes = repository.getNotes(NoteOrder.Date(OrderType.Descending)).first()
        return notes
            .filter { !it.noteEntity.isDeleted }
            .map { it.noteEntity.toAppFunctionNote() }
    }

    suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        title: String,
        content: String,
    ): Note {
        val repository = repository(appFunctionContext)
        val entity = NoteEntity(
            title = title,
            content = content,
            timestamp = System.currentTimeMillis(),
        )
        val id = repository.insertNote(entity, emptyList())
        return entity.copy(id = id).toAppFunctionNote()
    }

    suspend fun editNote(
        appFunctionContext: AppFunctionContext,
        noteId: String,
        title: String?,
        content: String?,
    ): Note? {
        val repository = repository(appFunctionContext)
        val existing = repository.getNoteById(noteId)?.noteEntity ?: return null
        val updated = existing.copy(
            title = title ?: existing.title,
            content = content ?: existing.content,
            lastUpdateTime = System.currentTimeMillis(),
            isSynced = false,
        )
        repository.updateNoteEntity(updated)
        return updated.toAppFunctionNote()
    }

    suspend fun deleteNote(
        appFunctionContext: AppFunctionContext,
        noteId: String,
    ): Boolean {
        val repository = repository(appFunctionContext)
        repository.getNoteById(noteId) ?: return false
        repository.deleteNoteWithId(noteId)
        return true
    }

    private fun repository(appFunctionContext: AppFunctionContext) =
        EntryPointAccessors
            .fromApplication(
                appFunctionContext.context.applicationContext,
                AppFunctionsEntryPoint::class.java,
            )
            .noteRepository()
}

private fun NoteEntity.toAppFunctionNote() = Note(
    id = id,
    title = title,
    content = content,
)
