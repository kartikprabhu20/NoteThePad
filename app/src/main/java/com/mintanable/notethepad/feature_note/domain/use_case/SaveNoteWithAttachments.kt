package com.mintanable.notethepad.feature_note.domain.use_case

import android.content.Context
import android.net.Uri
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_note.domain.model.InvalidNoteException
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.presentation.notes.util.AttachmentHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.jvm.Throws

class SaveNoteWithAttachments(
    private val repository: NoteRepository,
    private val fileManager: FileManager,
    @ApplicationContext val context: Context
) {

    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(note: Note){
        if(note.title.isBlank()){
            throw InvalidNoteException("The title of the note cant be empty")
        }
        repository.insertNote(note)
    }

    @Throws(InvalidNoteException::class)
    suspend operator fun invoke( id: Int?,
                                 title: String,
                                 content: String,
                                 timestamp: Long,
                                 color: Int,
                                 imageUris: List<Uri> = emptyList(),
                                 audioUris: List<Uri> = emptyList()) {
        if(title.isBlank()){
            throw InvalidNoteException("The title of the note cant be empty")
        }

        val imageUriList = imageUris.mapNotNull { uri ->
            if (uri.toString().contains(context.packageName)) {
                uri.toString()
            } else {
                fileManager.saveMediaToStorage(uri, AttachmentHelper.getAttachmentType(context, uri).name.lowercase())
            }
        }

        val audioUriList = audioUris.mapNotNull { uri ->
            if (uri.toString().contains(context.packageName)) {
                uri.toString()
            } else {
                fileManager.saveMediaToStorage(uri, AttachmentHelper.getAttachmentType(context, uri).name.lowercase())
            }
        }

        repository.insertNote(Note(
            id = id,
            title = title,
            content = content,
            timestamp = timestamp,
            color = color,
            imageUris = imageUriList,
            audioUris = audioUriList
        ))
    }
}