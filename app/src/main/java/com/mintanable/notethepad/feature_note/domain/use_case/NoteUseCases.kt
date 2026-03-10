package com.mintanable.notethepad.feature_note.domain.use_case

data class NoteUseCases(
    val getDetailedNotes: GetDetailedNotes,
    val getNotesWithReminders: GetNotesWithReminders,
    val getNotesWithTags: GetNotesWithTag,
    val deleteNote: DeleteNote,
    val saveNoteWithAttachments: SaveNoteWithAttachments,
    val getDetailedNote: GetDetailedNote,
    val getTopNotes: GetTopNotes
)
