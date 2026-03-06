package com.mintanable.notethepad.feature_note.domain.use_case

data class NoteUseCases(
    val getDetailedNotes: GetDetailedNotes,
    val deleteNote: DeleteNote,
    val saveNoteWithAttachments: SaveNoteWithAttachments,
    val getDetailedNote: GetDetailedNote,
    val getTopNotes: GetTopNotes
)
