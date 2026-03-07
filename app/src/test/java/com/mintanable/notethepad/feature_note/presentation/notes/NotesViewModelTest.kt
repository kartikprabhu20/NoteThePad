package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.core.net.toUri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import com.mintanable.notethepad.feature_settings.domain.use_case.GetLayoutSettings
import com.mintanable.notethepad.feature_settings.domain.use_case.ToggleLayoutSettings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val noteUseCases = mockk<NoteUseCases>(relaxed = true)
    private val getLayoutSettings = mockk<GetLayoutSettings>(relaxed = true)
    private val toggleLayoutSettings = mockk<ToggleLayoutSettings>(relaxed = true)
    private val fileIOUseCases = mockk<FileIOUseCases>(relaxed = true)

    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getLayoutSettings() } returns flowOf(true)
        viewModel = NotesViewModel(
            noteUseCases, getLayoutSettings, toggleLayoutSettings, fileIOUseCases
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Search query updates state after debounce`() = runTest {
        val notes = listOf(DetailedNote(
            id = 1,
            title = "Testing Title",
            content = "Testing Content",
            audioAttachments = emptyList(),
            imageUris = emptyList(),
            color = 0,
            timestamp = 0L,
            isCheckboxListAvailable = false,
            checkListItems = emptyList(),
            reminderTime = -1L
        ))
        every { noteUseCases.getDetailedNotes(any()) } returns flowOf(notes)

        viewModel.state.test {
            awaitItem() // Initial empty state

            viewModel.onEvent(NotesEvent.SearchBarValueChange("Test"))
            advanceTimeBy(301)
            runCurrent()

            val state = awaitItem()
            assertThat(state.notes).hasSize(1)
        }
    }


    @Test
    fun `Changing note order triggers new database fetch`() = runTest {
        val newOrder = NoteOrder.Title(OrderType.Ascending)
        every { noteUseCases.getDetailedNotes(any()) } returns flowOf(emptyList())
        viewModel.onEvent(NotesEvent.Order(newOrder))
        viewModel.state.test {
            awaitItem() // Initial state
            verify(exactly = 1) { noteUseCases.getDetailedNotes(newOrder) }
        }
    }

    @Test
    fun `DeleteNote deletes files and note, then RestoreNote re-saves it`() = runTest {
        val note = DetailedNote(
            id = 1,
            title = "Testing Title",
            content = "Testing Content",
            audioAttachments = emptyList(),
            imageUris = listOf("content://media/1".toUri()),
            color = 0,
            timestamp = 0L,
            isCheckboxListAvailable = false,
            checkListItems = emptyList(),
            reminderTime = -1L
        )

        viewModel.onEvent(NotesEvent.DeleteNote(note))
        advanceUntilIdle()

        coVerify { fileIOUseCases.deleteFiles(listOf("content://media/1")) }
        coVerify { noteUseCases.deleteNote.invoke(any<Note>()) }

        viewModel.onEvent(NotesEvent.RestoreNote)
        advanceUntilIdle()

        coVerify { noteUseCases.saveNoteWithAttachments(any()) }
    }

    @Test
    fun `DeleteNote emits Snackbar with working Undo action`() = runTest {
        val note = DetailedNote(
            id = 1,
            title = "Testing Title",
            content = "Testing Content",
            audioAttachments = emptyList(),
            imageUris = emptyList(),
            color = 0,
            timestamp = 0L,
            isCheckboxListAvailable = false,
            checkListItems = emptyList(),
            reminderTime = -1L
        )

        viewModel.eventFlow.test {
            viewModel.onEvent(NotesEvent.DeleteNote(note))

            val event = awaitItem() as NotesViewModel.UiEvent.ShowSnackbar
            assertThat(event.message).isEqualTo("Note deleted")

            // Act: Manually trigger the Undo action inside the snackbar
            event.onAction?.invoke()
            advanceUntilIdle()

            // Assert: Verify the note was saved again
            coVerify { noteUseCases.saveNoteWithAttachments(any()) }
        }
    }
}