package com.mintanable.notethepad.feature_note.domain.use_case

import androidx.core.net.toUri
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.Attachment
import com.mintanable.notethepad.feature_note.domain.util.CheckboxConvertors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class GetDetailedNote(
    private val repository: NoteRepository,
    private val audioMetadataProvider: AudioMetadataProvider,
) {

    suspend operator fun invoke(id:Long): DetailedNote? = withContext(Dispatchers.IO){
        val note = repository.getNoteById(id) ?: return@withContext null

        val audioAttachments = coroutineScope {
            note.audioUris.map { uriString ->
                async {
                    val uri = uriString.toUri()
                    Attachment(uri, audioMetadataProvider.getDuration(uri))
                }
            }.awaitAll()
        }
        val isCheckboxListAvailable = CheckboxConvertors.isContentCheckboxList(note.content)
        val checkList = if(isCheckboxListAvailable) CheckboxConvertors.stringToCheckboxes(content = note.content) else emptyList()

        DetailedNote(
            id = note.id,
            title = note.title,
            content = note.content,
            timestamp = note.timestamp,
            color = note.color,
            imageUris = note.imageUris.map { it.toUri() },
            audioAttachments = audioAttachments,
            reminderTime = note.reminderTime,
            checkListItems = checkList,
            isCheckboxListAvailable = isCheckboxListAvailable
        )
    }

}