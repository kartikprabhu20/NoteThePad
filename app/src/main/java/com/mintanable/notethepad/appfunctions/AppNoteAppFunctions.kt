package com.mintanable.notethepad.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import com.mintanable.notethepad.feature_appfunctions.Note
import com.mintanable.notethepad.feature_appfunctions.NoteAppFunctions
import javax.inject.Inject

/**
 * The actual `@AppFunction` surface published by NoteThePad.
 *
 * The annotations have to live in the `:app` source set so the KSP aggregator
 * (`appfunctions:aggregateAppFunctions = true`) emits a non-empty invoker
 * registry. The real work lives in [NoteAppFunctions] inside `:feature:appfunctions`.
 */
class AppNoteAppFunctions @Inject constructor() {

    private val delegate = NoteAppFunctions()

    /**
     * Lists all available notes.
     *
     * @param appFunctionContext The context in which the AppFunction is executed.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listNotes(appFunctionContext: AppFunctionContext): List<Note> =
        delegate.listNotes(appFunctionContext)

    /**
     * Adds a new note to NoteThePad.
     *
     * @param appFunctionContext The context in which the AppFunction is executed.
     * @param title The title of the note.
     * @param content The note's content.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        title: String,
        content: String,
    ): Note = delegate.createNote(appFunctionContext, title, content)

    /**
     * Edits a single note.
     *
     * @param appFunctionContext The context in which the AppFunction is executed.
     * @param noteId The target note's ID.
     * @param title The note's new title, or null to leave it unchanged.
     * @param content The note's new content, or null to leave it unchanged.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun editNote(
        appFunctionContext: AppFunctionContext,
        noteId: String,
        title: String?,
        content: String?,
    ): Note? = delegate.editNote(appFunctionContext, noteId, title, content)

    /**
     * Deletes a single note.
     *
     * @param appFunctionContext The context in which the AppFunction is executed.
     * @param noteId The target note's ID.
     * @return Whether the note was deleted or not.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteNote(
        appFunctionContext: AppFunctionContext,
        noteId: String,
    ): Boolean = delegate.deleteNote(appFunctionContext, noteId)
}
