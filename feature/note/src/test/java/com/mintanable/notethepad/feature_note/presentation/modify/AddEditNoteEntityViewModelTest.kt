package com.mintanable.notethepad.feature_note.presentation.modify

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.model.note.MediaState
import com.mintanable.notethepad.core.richtext.model.SpanType
import com.mintanable.notethepad.database.db.entity.AttachmentType
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.database.db.entity.InvalidNoteException
import com.mintanable.notethepad.database.db.repository.CollaborationRepository
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_ai.domain.use_cases.AnalyzeImageUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetAiModelByName
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetAutoTagsUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.QueryImageUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.StartLiveTransctiption
import com.mintanable.notethepad.feature_ai.domain.use_cases.StopLiveTranscription
import com.mintanable.notethepad.feature_ai.domain.use_cases.TranscribeAudioFileUseCase
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.core.common.ReminderScheduler
import com.mintanable.notethepad.feature_note.domain.use_case.fileio.FileIOUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.notes.NoteUseCases
import com.mintanable.notethepad.feature_note.domain.use_case.permissions.PermissionUsecases
import com.mintanable.notethepad.feature_note.domain.use_case.tags.TagUseCases
import com.mintanable.notethepad.permissions.DeniedType
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

    // Mocks
    private val noteUseCases = mockk<NoteUseCases>(relaxed = true)
    private val savedStateHandle = SavedStateHandle()
    private val permissionUsecases = mockk<PermissionUsecases>(relaxed = true)
    private val audioRecorder = mockk<AudioRecorder>(relaxed = true)
    private val fileIOUseCases = mockk<FileIOUseCases>(relaxed = true)
    private val mediaPlayer = mockk<MediaPlayer>(relaxed = true)
    private val audioMetadataProvider = mockk<AudioMetadataProvider>(relaxed = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)
    private val tagUseCases = mockk<TagUseCases>(relaxed = true)
    private val getAutoTagsUseCase = mockk<GetAutoTagsUseCase>(relaxed = true)
    private val startLiveTranscription = mockk<StartLiveTransctiption>(relaxed = true)
    private val stopLiveTranscription = mockk<StopLiveTranscription>(relaxed = true)
    private val transcribeAudioFileUseCase = mockk<TranscribeAudioFileUseCase>(relaxed = true)
    private val analyzeImageUseCase = mockk<AnalyzeImageUseCase>(relaxed = true)
    private val queryImageUseCase = mockk<QueryImageUseCase>(relaxed = true)

    private val authRepository = mockk<AuthRepository>(relaxed=true)
    private val collaborationRepository = mockk<CollaborationRepository>(relaxed=true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val getAiModelByName = mockk<GetAiModelByName>(relaxed = true)

    private val appContext = mockk<Context>(relaxed = true)

    private lateinit var viewModel: AddEditNoteViewModel

    private fun createViewModel(handle: SavedStateHandle = savedStateHandle) = AddEditNoteViewModel(
        noteUseCases, handle, permissionUsecases, audioRecorder,
        fileIOUseCases, mediaPlayer, audioMetadataProvider, reminderScheduler,
        tagUseCases, getAutoTagsUseCase, startLiveTranscription, stopLiveTranscription,
        transcribeAudioFileUseCase, analyzeImageUseCase, queryImageUseCase,
        authRepository, collaborationRepository, userPreferencesRepository, getAiModelByName,appContext
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)

        every { mediaPlayer.mediaState } returns MutableStateFlow(MediaState())
        every { audioRecorder.amplitude } returns MutableStateFlow(0)

        viewModel = createViewModel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    // ── Load note ───────────────────────────────────────────────────────

    @Test
    fun `When Note ID is passed, UI State loads correct data`() = runTest {
        val noteId = "1L"
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

        viewModel = createViewModel(realSavedStateHandle)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.titleState.richText.rawText).isEqualTo("")
            val loadedState = awaitItem()
            assertThat(loadedState.titleState.richText.rawText).isEqualTo("Testing Title")
        }
    }

    // ── Title events ────────────────────────────────────────────────────

    @Test
    fun `Entering title updates titleState and hides hint`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.EnteredTitle("New Note Title"))
            val state = awaitItem()
            assertThat(state.titleState.richText.rawText).isEqualTo("New Note Title")
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

    // ── Content events ──────────────────────────────────────────────────

    @Test
    fun `Entering content updates contentRichTextState document`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.EnteredContent(TextFieldValue("New content", TextRange(11))))
            val state = awaitItem()
            assertThat(state.contentRichTextState.document.rawText).isEqualTo("New content")
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
    fun `Losing focus on content clears pending styles`() = runTest {
        val focused = object : FocusState {
            override val isFocused = true; override val hasFocus = true; override val isCaptured = false
        }
        val unfocused = object : FocusState {
            override val isFocused = false; override val hasFocus = false; override val isCaptured = false
        }

        viewModel.uiState.test {
            awaitItem()
            // Focus and apply bold
            viewModel.onEvent(AddEditNoteEvent.ChangeContentFocus(focused))
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.BOLD))
            val boldState = awaitItem()
            assertThat(boldState.contentRichTextState.pendingStyles).contains(SpanType.BOLD)

            // Unfocus
            viewModel.onEvent(AddEditNoteEvent.ChangeContentFocus(unfocused))
            val unfocusedState = awaitItem()
            assertThat(unfocusedState.contentRichTextState.pendingStyles).isEmpty()
            assertThat(unfocusedState.contentRichTextState.pendingBlockType).isNull()
            assertThat(unfocusedState.contentRichTextState.pendingBullet).isFalse()
        }
    }

    // ── Rich text formatting ────────────────────────────────────────────

    @Test
    fun `ApplyContentFormat toggles bold pending style on empty content`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.BOLD))
            val state = awaitItem()
            assertThat(state.contentRichTextState.pendingStyles).contains(SpanType.BOLD)
        }
    }

    @Test
    fun `ApplyContentFormat H1 sets pending block type on empty content`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.H1))
            val state = awaitItem()
            assertThat(state.contentRichTextState.pendingBlockType).isEqualTo(SpanType.H1)
        }
    }

    @Test
    fun `ApplyContentFormat BULLET sets pending bullet on empty content`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.BULLET))
            val state = awaitItem()
            assertThat(state.contentRichTextState.pendingBullet).isTrue()
        }
    }

    @Test
    fun `Typing after setting pending bold applies bold to typed text`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            // Set bold pending
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.BOLD))
            awaitItem()

            // Type text
            viewModel.onEvent(AddEditNoteEvent.EnteredContent(TextFieldValue("Hello", TextRange(5))))
            val state = awaitItem()
            assertThat(state.contentRichTextState.document.isActiveAt(SpanType.BOLD, 0, 5)).isTrue()
        }
    }

    @Test
    fun `Typing after setting pending H1 applies H1 to typed line`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.H1))
            awaitItem()

            viewModel.onEvent(AddEditNoteEvent.EnteredContent(TextFieldValue("Title", TextRange(5))))
            val state = awaitItem()
            assertThat(state.contentRichTextState.document.isActiveAt(SpanType.H1, 0, 5)).isTrue()
        }
    }

    @Test
    fun `Typing after setting pending bullet inserts bullet prefix and span`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.BULLET))
            awaitItem()

            viewModel.onEvent(AddEditNoteEvent.EnteredContent(TextFieldValue("Item", TextRange(4))))
            val state = awaitItem()
            assertThat(state.contentRichTextState.document.rawText).contains("Item")
            assertThat(state.contentRichTextState.pendingBullet).isTrue()
        }
    }

    @Test
    fun `ApplyContentFormat on non-empty content applies block style to current line`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            // Type some content first
            viewModel.onEvent(AddEditNoteEvent.EnteredContent(TextFieldValue("Hello World", TextRange(11))))
            awaitItem()

            // Apply H1
            viewModel.onEvent(AddEditNoteEvent.ApplyContentFormat(SpanType.H1))
            val state = awaitItem()
            assertThat(state.contentRichTextState.document.isActiveAt(SpanType.H1, 0, 11)).isTrue()
        }
    }

    @Test
    fun `Toggle rich text bar updates isRichTextBarActive`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ToggleRichTextBar)
            assertThat(awaitItem().isRichTextBarActive).isTrue()
            viewModel.onEvent(AddEditNoteEvent.ToggleRichTextBar)
            assertThat(awaitItem().isRichTextBarActive).isFalse()
        }
    }

    // ── AttachTranscript (now goes through RichTextEngine) ──────────────

    @Test
    fun `AttachTranscript appends text to content via RichTextEngine`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            // Type initial content
            viewModel.onEvent(AddEditNoteEvent.EnteredContent(TextFieldValue("Existing", TextRange(8))))
            awaitItem()

            // Attach transcript
            viewModel.onEvent(AddEditNoteEvent.AttachTranscript("transcribed text"))
            val state = awaitItem()
            assertThat(state.contentRichTextState.document.rawText).contains("Existing")
            assertThat(state.contentRichTextState.document.rawText).contains("transcribed text")
            assertThat(state.contentState.isHintVisible).isFalse()
        }
    }

    @Test
    fun `AttachTranscript on empty content does not prepend newline`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.AttachTranscript("first transcript"))
            val state = awaitItem()
            assertThat(state.contentRichTextState.document.rawText).doesNotMatch("^\n.*")
            assertThat(state.contentRichTextState.document.rawText).contains("first transcript")
        }
    }

    // ── Save / Delete / Copy / Pin ──────────────────────────────────────

    @Test
    fun `Saving a note emits SaveNote event on success`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.success("1L")
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onEvent(AddEditNoteEvent.SaveNote)
                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }
            assertThat(awaitItem()).isInstanceOf(UiEvent.SaveNote::class.java)
        }
    }

    @Test
    fun `Saving a note emits Snackbar event on failure`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.failure(InvalidNoteException("The title of the note cant be empty"))
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onEvent(AddEditNoteEvent.SaveNote)
                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }
            assertThat(awaitItem()).isInstanceOf(UiEvent.ShowSnackbar::class.java)
        }
    }

    @Test
    fun `MakeCopy emits MakeCopy event on success`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.success("1L")
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onEvent(AddEditNoteEvent.MakeCopy)
                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }
            assertThat(awaitItem()).isInstanceOf(UiEvent.MakeCopy::class.java)
        }
    }

    @Test
    fun `MakeCopy emits Snackbar event on failure`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.failure(InvalidNoteException("The title of the note cant be empty"))
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onEvent(AddEditNoteEvent.MakeCopy)
                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }
            assertThat(awaitItem()).isInstanceOf(UiEvent.ShowSnackbar::class.java)
        }
    }

    @Test
    fun `Delete a note emits Delete event`() = runTest {
        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditNoteEvent.DeleteNote)
            assertThat(awaitItem()).isInstanceOf(UiEvent.DeleteNote::class.java)
        }
    }

    @Test
    fun `Pinning a note emits RequestWidgetPin event on success`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.success("1L")
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onEvent(AddEditNoteEvent.PinNote)
                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }
            assertThat(awaitItem()).isInstanceOf(UiEvent.RequestWidgetPin::class.java)
        }
    }

    @Test
    fun `Pinning a note emits Snackbar error event on failure`() = runTest {
        coEvery {
            noteUseCases.saveNoteWithAttachments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(100)
            Result.failure(InvalidNoteException(""))
        }
        viewModel.eventFlow.test {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onEvent(AddEditNoteEvent.PinNote)
                assertThat(awaitItem().isSaving).isTrue()
                assertThat(awaitItem().isSaving).isFalse()
            }
            assertThat(awaitItem()).isInstanceOf(UiEvent.ShowSnackbar::class.java)
        }
    }

    // ── Color ───────────────────────────────────────────────────────────

    @Test
    fun `Change color triggers change in color in uistate`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ChangeColor(1))
            val state = awaitItem()
            assertThat(state.noteColor).isEqualTo(1)
        }
    }

    // ── Attachments ─────────────────────────────────────────────────────

    @Test
    fun `AttachImage adds uri to list only if not already present`() = runTest {
        val uri = "content://media/1".toUri()
        viewModel.uiState.test {
            awaitItem()
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
            awaitItem()
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
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.RemoveImage(uri))
            assertThat(awaitItem().attachedImages).doesNotContain(uri)
            coVerify { fileIOUseCases.deleteFiles(listOf(uri.toString())) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RemoveAudio stops player if the removed audio was playing`() = runTest {
        val uriString = "content://audio/1"
        val playingState = MediaState(currentUri = uriString, isPlaying = true)
        every { mediaPlayer.mediaState } returns MutableStateFlow(playingState)

        viewModel.uiState.test {
            val currentState = awaitItem()
            if (currentState.mediaState?.currentUri != uriString) awaitItem()

            viewModel.onEvent(AddEditNoteEvent.RemoveAudio(uriString))
            advanceUntilIdle()
            verify(exactly = 1) { mediaPlayer.stop() }
        }
    }

    // ── Reminders ───────────────────────────────────────────────────────

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CancelReminder updates state and cancels scheduled alarm`() = runTest {
        val noteId = "1L"
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
        viewModel = createViewModel(handle)
        advanceUntilIdle()
        viewModel.onEvent(AddEditNoteEvent.CancelReminder)

        assertThat(viewModel.uiState.value.reminderTime).isEqualTo(-1L)
        verify(exactly = 1) { reminderScheduler.cancel(noteId.hashCode().toLong()) }
    }

    // ── Checkbox ────────────────────────────────────────────────────────

    @Test
    fun `ToggleCheckbox converts string content to checklist items`() = runTest {
        viewModel.onEvent(AddEditNoteEvent.EnteredContent(TextFieldValue("Line 1\nLine 2", TextRange(13))))

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.ToggleCheckbox)
            val state = awaitItem()
            assertThat(state.isCheckboxListAvailable).isTrue()
            assertThat(state.checkListItems).hasSize(2)
            assertThat(state.contentRichTextState.document.rawText).isEmpty()
        }
    }

    @Test
    fun `AddChecklistItem inserts new item after the specified item`() = runTest {
        val itemA = CheckboxItem(id = "A", text = "A")
        val itemB = CheckboxItem(id = "B", text = "B")
        viewModel.onEvent(AddEditNoteEvent.UpdateCheckList(listOf(itemA, itemB)))

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(AddEditNoteEvent.AddChecklistItem(itemA))
            val newList = awaitItem().checkListItems
            assertThat(newList).hasSize(3)
            assertThat(newList[0].id).isEqualTo("A")
            assertThat(newList[1].text).isEmpty()
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
            assertThat(finalState.contentRichTextState.document.rawText).contains("line 1")
            assertThat(finalState.contentRichTextState.document.rawText).contains("line 2")
        }
    }

    // ── Audio recording ─────────────────────────────────────────────────

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
            awaitItem()

            viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording(enableLiveTranscription = false))
            advanceUntilIdle()

            val startState = awaitItem()
            assertThat(startState.isRecording).isTrue()
            verify { audioRecorder.startRecording(mockFile) }

            viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording(enableLiveTranscription = false))
            advanceUntilIdle()

            val finalState = expectMostRecentItem()
            assertThat(finalState.isRecording).isFalse()
            assertThat(finalState.attachedAudios).hasSize(1)
            assertThat(finalState.attachedAudios[0].duration).isEqualTo(1234)
        }
        unmockkStatic(Uri::class)
    }

    // ── Permissions ─────────────────────────────────────────────────────

    @Test
    fun `Microphone permission granted emits LaunchAudioRecorder event`() = runTest {
        viewModel.eventFlow.test {
            viewModel.checkMicrophonePermission(isGranted = true, shouldShowRationale = false)
            assertThat(awaitItem()).isInstanceOf(UiEvent.LaunchAudioRecorder::class.java)
        }
    }

    @Test
    fun `Microphone permission denied with rationale updates showMicrophoneRationale`() = runTest {
        viewModel.uiState.test {
            awaitItem()
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
            val event = awaitItem() as UiEvent.LaunchCamera
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
