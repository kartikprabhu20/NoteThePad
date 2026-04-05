package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.TestDispatcherProvider
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.common.NotesFilterType
import com.mintanable.notethepad.core.common.WidgetRefresher
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.feature_note.domain.use_case.GetNoteShapeSettings
import com.mintanable.notethepad.feature_note.domain.use_case.GetSupaSyncSettings
import com.mintanable.notethepad.feature_note.domain.use_case.GetSupaSyncStatus
import com.mintanable.notethepad.feature_note.domain.use_case.RefreshSupaSync
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.notes.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.tags.TagUseCases
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
    private val noteUseCases = mockk<NoteUseCases>(relaxed = true)
    private val getNoteShapeSettings = mockk<GetNoteShapeSettings>(relaxed = true)
    private val fileIOUseCases = mockk<FileIOUseCases>(relaxed = true)
    private val tagUseCases = mockk<TagUseCases>(relaxed = true)

    private val savedStateHandle = SavedStateHandle()
    private val widgetRefresher = mockk< WidgetRefresher>(relaxed = true)
    private val getSupaSyncSettings =  mockk<GetSupaSyncSettings>(relaxed = true)
    private val getSupaSyncStatus =  mockk<GetSupaSyncStatus>(relaxed = true)
    private val refreshSupaSync = mockk<RefreshSupaSync>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)


    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getNoteShapeSettings() } returns flowOf(NoteShape.DEFAULT)
        viewModel = NotesViewModel(
            savedStateHandle = savedStateHandle,
            noteUseCases = noteUseCases,
            tagUseCases = tagUseCases,
            getNoteShapeSettings = getNoteShapeSettings,
            fileIOUseCases = fileIOUseCases,
            dispatchers = testDispatcherProvider,
            widgetRefresher = widgetRefresher,
            getSupaSyncSettings = getSupaSyncSettings,
            refreshSupaSync = refreshSupaSync,
            getSupaSyncStatus = getSupaSyncStatus,
            authRepository = authRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Search query updates state after debounce`() = runTest {
        val notes = listOf(DetailedNote(
            id = "1",
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

            val state = awaitItem()
            assertThat(state.notes).hasSize(1)
            assertThat(state.notes[0].title).isEqualTo("Testing Title")
        }
    }

    @Test
    fun `Changing filter via updateFilter triggers new database fetch`() = runTest {
        val tagEntity = TagEntity(tagId = "10L", tagName = "Work")
        val tagNotes = listOf(DetailedNote(
            id = "1",
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

        every { noteUseCases.getNotesWithTags(any(), any()) } returns flowOf(tagNotes)

        viewModel.state.test {
            awaitItem() // Skip initial fetch

            viewModel.updateFilter(
                filter = NotesFilterType.TAGS.filter,
                tagId = tagEntity.tagId,
                tagName = tagEntity.tagName
            )

            val state = awaitItem()
            assertThat(state.notes).isEqualTo(tagNotes)
            verify { noteUseCases.getNotesWithTags(any(), match { it.tagId == tagEntity.tagId && it.tagName == tagEntity.tagName }) }
        }
    }

    @Test
    fun `Changing note order triggers new database fetch`() = runTest {
        val newOrder = NoteOrder.Title(OrderType.Ascending)
        val mockNotes = listOf(DetailedNote(
            id = "1",
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
        every { noteUseCases.getDetailedNotes(any()) } returns flowOf(emptyList()) // Initial
        every { noteUseCases.getDetailedNotes(newOrder) } returns flowOf(mockNotes) // After change

        viewModel.state.test {
            awaitItem()
            viewModel.onEvent(NotesEvent.Order(newOrder))
            testScheduler.advanceTimeBy(301L)
            runCurrent()
            val finalState = awaitItem()
            assertThat(finalState.notes).isEqualTo(mockNotes)
            verify { noteUseCases.getDetailedNotes(newOrder) }
        }
    }

    @Test
    fun `DeleteNote deletes files and note, then RestoreNote re-saves it`() = runTest {
        val note = DetailedNote(
            id = "1",
            title = "Testing Title",
            content = "Testing Content",
            audioAttachments = emptyList(),
            imageUris = listOf("content://media/1"),
            color = 0,
            timestamp = 0L,
            isCheckboxListAvailable = false,
            checkListItems = emptyList(),
            reminderTime = -1L
        )

        viewModel.onEvent(NotesEvent.DeleteNote(note))
        advanceUntilIdle()

        coVerify { fileIOUseCases.deleteFiles(listOf("content://media/1")) }
        coVerify { noteUseCases.deleteNote.invoke(any<NoteEntity>()) }

        viewModel.onEvent(NotesEvent.RestoreNote)
        advanceUntilIdle()

        coVerify { noteUseCases.saveNoteWithAttachments(any()) }
    }

    @Test
    fun `DeleteNote emits Snackbar with working Undo action`() = runTest {
        val note = DetailedNote(
            id = "1",
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