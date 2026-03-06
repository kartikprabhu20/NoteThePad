package com.mintanable.notethepad.feature_note.domain.use_case

import androidx.core.net.toUri
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.Attachment
import com.mintanable.notethepad.feature_note.domain.util.CheckboxConvertors
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetDetailedNotes(
    private val repository: NoteRepository,
    private val audioMetadataProvider: AudioMetadataProvider
) {

    operator fun invoke(order: NoteOrder): Flow<List<DetailedNote>> {
        return repository.getNotes(order).map { notesList ->
            coroutineScope {
                notesList.map { note ->
                    async {
                        val audioAttachments = note.audioUris.map { uriString ->
                            val uri = uriString.toUri()
                            Attachment(uri, audioMetadataProvider.getDuration(uri))
                        }

                        val isCheckbox = CheckboxConvertors.isContentCheckboxList(note.content)

                        DetailedNote(
                            id = note.id,
                            title = note.title,
                            content = note.content,
                            timestamp = note.timestamp,
                            color = note.color,
                            imageUris = note.imageUris.map { it.toUri() },
                            audioAttachments = audioAttachments,
                            reminderTime = note.reminderTime,
                            checkListItems = if (isCheckbox) CheckboxConvertors.stringToCheckboxes(note.content) else emptyList(),
                            isCheckboxListAvailable = isCheckbox
                        )
                    }
                }.awaitAll()
            }
        }
    }
}