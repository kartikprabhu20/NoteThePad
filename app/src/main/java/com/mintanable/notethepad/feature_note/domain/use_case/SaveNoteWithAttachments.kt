package com.mintanable.notethepad.feature_note.domain.use_case

import android.content.Context
import android.net.Uri
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem
import com.mintanable.notethepad.feature_note.domain.model.InvalidNoteException
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.CheckboxConvertors
import com.mintanable.notethepad.feature_note.presentation.notes.util.AttachmentHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.jvm.Throws

class SaveNoteWithAttachments(
    private val repository: NoteRepository,
    private val fileManager: FileManager,
    @ApplicationContext val context: Context
) {

    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(note: Note) : Long{
        if(note.title.isBlank()){
            throw InvalidNoteException("The title of the note cant be empty")
        }
        return repository.insertNote(note)
    }

    suspend operator fun invoke(
        id: Long?,
        title: String,
        content: String,
        timestamp: Long,
        color: Int,
        imageUris: List<Uri> = emptyList(),
        audioUris: List<Uri> = emptyList(),
        reminderTime: Long,
        checkboxItems: List<CheckboxItem>
    ) : Result<Long> {

       try{
            if(title.isBlank()){
                return Result.failure(InvalidNoteException("The title of the note cant be empty"))
            }

           val newContent = if(checkboxItems.isNotEmpty()) CheckboxConvertors.checkboxesToString(checkboxItems) else content

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

            val newNoteId = repository.insertNote(
                Note(
                    id = id,
                    title = title,
                    content = newContent,
                    timestamp = timestamp,
                    color = color,
                    imageUris = imageUriList,
                    audioUris = audioUriList,
                    reminderTime = reminderTime
                )
            )
            return Result.success(newNoteId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}