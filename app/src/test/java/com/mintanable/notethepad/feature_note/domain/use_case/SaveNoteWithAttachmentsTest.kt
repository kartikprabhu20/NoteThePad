package com.mintanable.notethepad.feature_note.domain.use_case

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.core.file.FileManager
import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem
import com.mintanable.notethepad.feature_note.domain.model.InvalidNoteException
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SaveNoteWithAttachmentsTest{
     private lateinit var saveNote: SaveNoteWithAttachments
     private val repository = mockk<NoteRepository>()
     private val fileManager = mockk<FileManager>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        saveNote = SaveNoteWithAttachments(repository, fileManager, context)
        coEvery { repository.insertNote(any()) } returns 1L
    }

    @Test
    fun `Title is blank, returns Result failure`() = runTest {
        val result = saveNote(
            id = null, title = "   ", content = "", timestamp = 0L,
            color = 0, reminderTime = 0L, checkboxItems = emptyList()
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidNoteException::class.java)

        coVerify(exactly = 0) { repository.insertNote(any()) }
    }

    @Test
    fun `External URIs are saved, Internal URIs are kept as is`() = runTest {

        val pkg = context.packageName
        val internalUri = Uri.parse("content://$pkg/files/my_image.jpg")
        val externalUri = Uri.parse("content://com.android.providers/other_image.jpg")
        val audioUri = Uri.parse("content://com.android.providers/test.mp4")

        val newSavedPath = "internal/storage/saved_image.jpg"

        // Mock FileManager only for the external one
        coEvery { fileManager.saveMediaToStorage(externalUri, any()) } returns newSavedPath

        val result = saveNote(
            id = null,
            title = "Valid Title",
            content = "Content",
            timestamp = 1L,
            color = 1,
            imageUris = listOf(internalUri, externalUri),
            audioUris = listOf(audioUri),
            reminderTime = 0L,
            checkboxItems = emptyList()
        )

        assertThat(result.isSuccess).isTrue()

        coVerify {
            repository.insertNote(match { note ->
                // Check if internal stayed the same and external was updated to saved path
                note.imageUris.contains(internalUri.toString()) &&
                        note.imageUris.contains(newSavedPath)
            })
        }

        // Verify fileManager was called EXACTLY once (only for the external one)
        coVerify(exactly = 1) { fileManager.saveMediaToStorage(any(), any()) }
    }

    @Test
    fun `Checkbox items presence overwrites content field`() = runTest {
        val checkboxes = listOf(CheckboxItem(text = "Buy Milk", isChecked = true))

        saveNote(
            id = null,
            title = "Grocery List",
            content = "Old Text Content",
            timestamp = 1L,
            color = 1,
            reminderTime = 0L,
            checkboxItems = checkboxes
        )

        coVerify {
            repository.insertNote(match { note ->
                // Note content should now be the converted checkbox string, not "Old Text Content"
                note.content != "Old Text Content"
            })
        }
    }
 }