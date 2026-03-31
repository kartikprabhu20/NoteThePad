package com.mintanable.notethepad.feature_note.presentation.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.database.db.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val deletedNotes: StateFlow<List<NoteWithTags>> = noteRepository.getDeletedNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.restoreNote(noteId)
        }
    }

    fun deleteNotePermanently(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNotePermanently(noteId)
        }
    }
}
