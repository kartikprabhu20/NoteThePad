package com.mintanable.notethepad.feature_note.domain.use_case

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.database.db.entity.AttachmentType
import com.mintanable.notethepad.database.db.entity.InvalidNoteException
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.use_case.notes.SaveNoteWithAttachments
import com.mintanable.notethepad.file.FileManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SaveNoteWithAttachmentsTest {
    private lateinit var saveNote: SaveNoteWithAttachments
    private val repository = mockk<NoteRepository>()
    private val fileManager = mockk<FileManager>()

    @Before
    fun setUp() {
        saveNote = SaveNoteWithAttachments(repository, fileManager)
        coEvery { repository.insertNote(any(), any()) } returns "1L"
    }

    @Test
    fun `Title is blank, returns Result failure`() = runTest {
        val result = saveNote(
            id = "0L", title = "   ", content = "", timestamp = 0L,
            color = 0, reminderTime = 0L, checkboxItems = emptyList()
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidNoteException::class.java)

        coVerify(exactly = 0) { repository.insertNote(any(), any()) }
    }

    @Test
    fun `External URIs are saved via FileManager`() = runTest {
        val externalImageUri = Uri.parse("content://com.android.providers/other_image.jpg")
        val externalAudioUri = Uri.parse("content://com.android.providers/test2.mp4")

        val newSavedPath1 = "/internal/storage/NoteAttachments/saved_image.jpg"
        val newSavedPath2 = "/internal/storage/NoteAttachments/test2.mp4"

        // Mock FileManager behavior
        coEvery { fileManager.getAttachmentType(any()) } returns AttachmentType.IMAGE
        coEvery { fileManager.saveMediaToStorage(externalImageUri, any()) } returns newSavedPath1
        coEvery { fileManager.saveMediaToStorage(externalAudioUri, any()) } returns newSavedPath2

        val result = saveNote(
            id = "0L",
            title = "Valid Title",
            content = "Content",
            timestamp = 1L,
            color = 1,
            imageUris = listOf(externalImageUri),
            audioUris = listOf(externalAudioUri),
            reminderTime = 0L,
            checkboxItems = emptyList()
        )

        assertThat(result.isSuccess).isTrue()

        coVerify {
            repository.insertNote(match { note ->
                note.imageUris.contains(newSavedPath1) &&
                        note.audioUris.contains(newSavedPath2)
            }, match { tags ->
                tags.isEmpty()
            })
        }

        coVerify(exactly = 2) { fileManager.saveMediaToStorage(any(), any()) }
    }

    @Test
    fun `Checkbox items presence overwrites content field`() = runTest {
        val checkboxes = listOf(CheckboxItem(text = "Buy Milk", isChecked = true))
        val expectedContent = CheckboxConvertors.checkboxesToString(checkboxes)

        saveNote(
            id = "0L",
            title = "Grocery List",
            content = "Old Text Content",
            timestamp = 1L,
            color = 1,
            reminderTime = 0L,
            checkboxItems = checkboxes,
            tagEntities = listOf(TagEntity("tag1"), TagEntity("tag2"))
        )

        coVerify {
            repository.insertNote(match { note ->
                // Note content should now be the converted checkbox string, not "Old Text Content"
                note.content == expectedContent
            }, match { tags ->
                tags.size == 2 && tags[0].tagName == "tag1" && tags[1].tagName == "tag2"
            })
        }
    }
}