package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.focus.FocusState
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.model.InvalidNoteException
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_note.domain.repository.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType
import com.mintanable.notethepad.feature_note.domain.util.MediaState
import com.mintanable.notethepad.feature_settings.presentation.use_cases.PermissionUsecases
import com.mintanable.notethepad.feature_settings.presentation.util.DeniedType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddEditNoteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    //Mocks
    private val noteUseCases = mockk<NoteUseCases>(relaxed = true)
    private val savedStateHandle = SavedStateHandle()
    private val context = mockk<Context>(relaxed = true)
    private val permissionUsecases = mockk<PermissionUsecases>(relaxed = true)
    private val audioRecorder = mockk<AudioRecorder>(relaxed = true)
    private val fileIOUseCases = mockk<FileIOUseCases>(relaxed = true)
    private val mediaPlayer = mockk<MediaPlayer>(relaxed = true)
    private val audioMetadataProvider = mockk<AudioMetadataProvider>(relaxed = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)

    private lateinit var viewModel: AddEditNoteViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)

        every { mediaPlayer.mediaState } returns MutableStateFlow(MediaState())

        viewModel = AddEditNoteViewModel(
            noteUseCases, savedStateHandle, context, permissionUsecases,
            audioRecorder, fileIOUseCases, mediaPlayer, audioMetadataProvider, reminderScheduler
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `When Note ID is passed, UI State loads correct data`() = runTest {

        val noteId = 1L
        val realSavedStateHandle = SavedStateHandle(mapOf("noteId" to noteId))

        val detailedNote = DetailedNote(
            id = noteId,
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

        coEvery { noteUseCases.getDetailedNote(noteId) } returns detailedNote

        viewModel = AddEditNoteViewModel(
            noteUseCases,
            realSavedStateHandle,
            context,
            permissionUsecases,
            audioRecorder,
            fileIOUseCases,
            mediaPlayer,
            audioMetadataProvider,
            reminderScheduler
        )

        viewModel.uiState.test {
            // Skip the initial empty state from stateIn(initialValue = ...)
            val initialState = awaitItem()
            assertThat(initialState.titleState.text).isEqualTo("")
            val loadedState = awaitItem()
            assertThat(loadedState.titleState.text).isEqualTo("Testing Title")
        }
    }


    @Test
    fun `Entering title updates titleState and hides hint`() = runTest {
        viewModel.uiState.test {
            //Skip initialValue
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.EnteredTitle("New Note Title"))
            val state = awaitItem()
            assertThat(state.titleState.text).isEqualTo("New Note Title")
            assertThat(state.titleState.isHintVisible).isFalse()
        }
    }

    @Test
    fun `Focussing on title updates titleState and hides hint`() = runTest {

        val focusedState = object : FocusState {
            override val isFocused: Boolean = true
            override val hasFocus: Boolean = true
            override val isCaptured: Boolean = false
        }
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ChangeTitleFocus(focusedState))
            val state = awaitItem()
            assertThat(state.titleState.isHintVisible).isFalse()
        }
    }

    @Test
    fun `Entering content updates contentState and hides hint`() = runTest {
        viewModel.uiState.test {
            //Skip initialValue
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.EnteredContent("New Note content"))
            val state = awaitItem()
            assertThat(state.contentState.text).isEqualTo("New Note content")
            assertThat(state.contentState.isHintVisible).isFalse()
        }
    }

    @Test
    fun `Focussing on content updates contentState and hides hint`() = runTest {

        val focusedState = object : FocusState {
            override val isFocused: Boolean = true
            override val hasFocus: Boolean = true
            override val isCaptured: Boolean = false
        }
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ChangeContentFocus(focusedState))
            val state = awaitItem()
            assertThat(state.contentState.isHintVisible).isFalse()
        }
    }

    @Test
    fun `Saving a note emits SaveNote event on success`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.success(1L)
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem() // Initial state from init

                viewModel.onEvent(AddEditNoteEvent.SaveNote)

                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }

            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.SaveNote::class.java)
        }
    }

    @Test
    fun `Saving a note emits SaveNote event on failure`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.failure(InvalidNoteException("The title of the note cant be empty"))
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem() // Initial state from init

                viewModel.onEvent(AddEditNoteEvent.SaveNote)

                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }

            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.ShowSnackbar::class.java)
        }
    }

    @Test
    fun `Saving a note emits MakeCopy event on success`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.success(1L)
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem() // Initial state from init

                viewModel.onEvent(AddEditNoteEvent.MakeCopy)

                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }

            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.MakeCopy::class.java)
        }
    }

    @Test
    fun `Saving a note emits MakeCopy event on failure`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.failure(InvalidNoteException("The title of the note cant be empty"))
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem() // Initial state from init

                viewModel.onEvent(AddEditNoteEvent.MakeCopy)

                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }

            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.ShowSnackbar::class.java)
        }
    }

    @Test
    fun `Delete a note emits Delete event`() = runTest {

        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditNoteEvent.DeleteNote)
            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.DeleteNote::class.java)
        }
    }


    @Test
    fun `Pinning a note emits RequestWidgetPin event on success`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.success(1L)
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem() // Initial state from init

                viewModel.onEvent(AddEditNoteEvent.PinNote)

                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }

            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.RequestWidgetPin::class.java)
        }
    }

    @Test
    fun `Pinning a note emits Snackbar error event on failure`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.failure(InvalidNoteException(""))
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem() // Initial state from init

                viewModel.onEvent(AddEditNoteEvent.MakeCopy)

                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }

            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.ShowSnackbar::class.java)
        }
    }



    @Test
    fun `Change color triggers change in color in uistate`() = runTest {

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ChangeColor(1))
            val state = awaitItem()
            assertThat(state.noteColor).isEqualTo(1)
        }
    }

    @Test
    fun `AttachImage adds uri to list only if not already present`() = runTest {
        val uri = "content://media/1".toUri()

        viewModel.uiState.test {
            awaitItem() // Skip initial

            // Act: Attach twice
            viewModel.onEvent(AddEditNoteEvent.AttachImage(uri))
            viewModel.onEvent(AddEditNoteEvent.AttachImage(uri))

            val state = awaitItem()
            assertThat(state.attachedImages).contains(uri)
            assertThat(state.attachedImages.count { it == uri }).isEqualTo(1)
        }
    }


    @Test
    fun `AttachVideo adds uri to list only if not already present`() = runTest {
        val uri = "content://media/1".toUri()

        viewModel.uiState.test {
            awaitItem() // Skip initial

            // Act: Attach twice
            viewModel.onEvent(AddEditNoteEvent.AttachVideo(uri))
            viewModel.onEvent(AddEditNoteEvent.AttachVideo(uri))

            val state = awaitItem()
            assertThat(state.attachedImages).contains(uri)
            assertThat(state.attachedImages.count { it == uri }).isEqualTo(1)
        }
    }

    @Test
    fun `RemoveImage updates state and triggers file deletion`() = runTest {
        val uri = "content://media/1".toUri()
        viewModel.onEvent(AddEditNoteEvent.AttachImage(uri))

        viewModel.uiState.test {
            awaitItem() // current state with image

            viewModel.onEvent(AddEditNoteEvent.RemoveImage(uri))

            assertThat(awaitItem().attachedImages).doesNotContain(uri)
            coVerify { fileIOUseCases.deleteFiles(listOf(uri.toString())) }
        }
    }

    @Test
    fun `RemoveAudio stops player if the removed audio was playing`() = runTest {
        val uriString = "content://audio/1"
        val uri = uriString.toUri()

        val playingState = MediaState(
            currentUri = uriString,
            isPlaying = true
        )
        every { mediaPlayer.mediaState } returns MutableStateFlow(playingState)

        viewModel.uiState.test {
            val currentState = awaitItem()
            val verifiedState = if (currentState.mediaState?.currentUri != uriString) {
                awaitItem()
            } else currentState

            viewModel.onEvent(AddEditNoteEvent.RemoveAudio(uri))
            advanceUntilIdle()
            verify(exactly = 1) { mediaPlayer.stop() }
        }
    }

    @Test
    fun `SetReminder updates state and clears permission rationale`() = runTest {
        val timestamp = 1700000000000L

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.SetReminder(timestamp))

            val state = awaitItem()
            assertThat(state.reminderTime).isEqualTo(timestamp)
            assertThat(state.showAlarmPermissionRationale).isFalse()
        }
    }

    @Test
    fun `CancelReminder updates state and cancels scheduled alarm`() = runTest {
        val noteId = 1L
        val handle = SavedStateHandle(mapOf("noteId" to noteId))
        val detailedNote = DetailedNote(
            id = noteId,
            title = "Testing Title",
            content = "Testing Content",
            audioAttachments = emptyList(),
            imageUris = emptyList(),
            color = 0,
            timestamp = 0L,
            isCheckboxListAvailable = false,
            checkListItems = emptyList(),
            reminderTime = 5000L
        )
        coEvery { noteUseCases.getDetailedNote(noteId) } returns detailedNote
        viewModel = AddEditNoteViewModel(noteUseCases, handle, context, permissionUsecases,
            audioRecorder, fileIOUseCases, mediaPlayer, audioMetadataProvider, reminderScheduler)
        advanceUntilIdle()
        viewModel.onEvent(AddEditNoteEvent.CancelReminder)

        assertThat(viewModel.uiState.value.reminderTime).isEqualTo(-1L)
        verify(exactly = 1) { reminderScheduler.cancel(noteId) }
    }

    @Test
    fun `ToggleCheckbox converts string content to checklist items`() = runTest {
        viewModel.onEvent(AddEditNoteEvent.EnteredContent("Line 1\nLine 2"))

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ToggleCheckbox)

            val state = awaitItem()
            assertThat(state.isCheckboxListAvailable).isTrue()
            assertThat(state.checkListItems).hasSize(2)
            assertThat(state.contentState.text).isEmpty()
        }
    }

    @Test
    fun `AddChecklistItem inserts new item after the specified item`() = runTest {
        // 1. Setup initial items
        val itemA = CheckboxItem(id = "A", text = "A")
        val itemB = CheckboxItem(id = "B", text = "B")
        viewModel.onEvent(AddEditNoteEvent.UpdateCheckList(listOf(itemA, itemB)))

        viewModel.uiState.test {
            awaitItem()

            // 2. Act: Add item after A
            viewModel.onEvent(AddEditNoteEvent.AddChecklistItem(itemA))

            val newList = awaitItem().checkListItems
            assertThat(newList).hasSize(3)
            assertThat(newList[0].id).isEqualTo("A")
            assertThat(newList[1].text).isEmpty() // The newly inserted item
            assertThat(newList[2].id).isEqualTo("B")
        }
    }

    @Test
    fun `ToggleCheckbox converts checklist items to string content`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ToggleCheckbox)
            val onState = awaitItem()
            assertThat(onState.isCheckboxListAvailable).isTrue()

            val itemA = CheckboxItem(id = "A", text = "line 1")
            val itemB = CheckboxItem(id = "B", text = "line 2")
            viewModel.onEvent(AddEditNoteEvent.UpdateCheckList(listOf(itemA, itemB)))
            awaitItem()

            viewModel.onEvent(AddEditNoteEvent.ToggleCheckbox)
            val finalState = awaitItem()
            assertThat(finalState.isCheckboxListAvailable).isFalse()
            assertThat(finalState.contentState.text).contains("line 1")
            assertThat(finalState.contentState.text).contains("line 2")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `ToggleAudioRecording starts and then stops recording, adding attachment to list`() = runTest {
        val mockFile = mockk<java.io.File>(relaxed = true)
        val mockUri = "file:///test_audio.mp3".toUri()

        coEvery { fileIOUseCases.createFile(any(), any()) } returns mockFile

        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns mockUri
        coEvery { audioMetadataProvider.getDuration(any()) } returns 1234

        viewModel.uiState.test {
            awaitItem() // Skip initial state

            viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)

            advanceUntilIdle()

            val startState = awaitItem()
            assertThat(startState.isRecording).isTrue()
            verify { audioRecorder.startRecording(mockFile) }

            viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)

            advanceUntilIdle()

            val finalState = expectMostRecentItem()
            assertThat(finalState.isRecording).isFalse()
            assertThat(finalState.attachedAudios).hasSize(1)
            assertThat(finalState.attachedAudios[0].duration).isEqualTo(1234)
        }
        unmockkStatic(Uri::class)
    }

    @Test
    fun `Microphone permission granted emits LaunchAudioRecorder event`() = runTest {
        viewModel.eventFlow.test {
            viewModel.checkMicrophonePermission(isGranted = true, shouldShowRationale = false)
            assertThat(awaitItem()).isInstanceOf(AddEditNoteViewModel.UiEvent.LaunchAudioRecorder::class.java)
        }
    }

    @Test
    fun `Microphone permission denied with rationale updates showMicrophoneRationale`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial
            viewModel.checkMicrophonePermission(isGranted = false, shouldShowRationale = true)
            assertThat(awaitItem().showMicrophoneRationale).isTrue()
        }
    }

    @Test
    fun `Microphone permission permanently denied updates settingsDeniedType`() = runTest {
        coEvery { permissionUsecases.getMicrophonePermissionFlag() } returns true

        viewModel.uiState.test {
            awaitItem()
            viewModel.checkMicrophonePermission(isGranted = false, shouldShowRationale = false)
            assertThat(awaitItem().settingsDeniedType).isEqualTo(DeniedType.MICROPHONE)
        }
    }

    @Test
    fun `Camera permission granted emits LaunchCamera event with correct type`() = runTest {
        val type = AttachmentType.IMAGE

        viewModel.eventFlow.test {
            viewModel.checkCameraPermission(isGranted = true, shouldShowRationale = false, attachmentType = type)

            val event = awaitItem() as AddEditNoteViewModel.UiEvent.LaunchCamera
            assertThat(event.type).isEqualTo(type)
        }
    }

    @Test
    fun `Exact alarm permission not granted shows rationale`() = runTest {
        every { reminderScheduler.canScheduleExactAlarms() } returns false

        viewModel.uiState.test {
            awaitItem()
            viewModel.checkExactAlarmPermission()

            assertThat(awaitItem().showAlarmPermissionRationale).isTrue()
        }
    }

    @Test
    fun `Exact alarm permission granted shows date time picker`() = runTest {
        every { reminderScheduler.canScheduleExactAlarms() } returns true

        viewModel.uiState.test {
            awaitItem()
            viewModel.checkExactAlarmPermission()

            val state = awaitItem()
            assertThat(state.showAlarmPermissionRationale).isFalse()
            assertThat(state.showDataAndTimePicker).isTrue()
        }
    }
}