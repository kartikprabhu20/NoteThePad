package com.mintanable.notethepad.feature_note.domain.use_case

import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteWithTags
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.DetailedNoteMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetDetailedNoteTest {

    private lateinit var getDetailedNote: GetDetailedNote
    private lateinit var detailedNoteMapper: DetailedNoteMapper

    // Mocks
    private val repository = mockk<NoteRepository>()
    private val audioMetadataProvider = mockk<AudioMetadataProvider>()

    @Before
    fun setUp() {
        detailedNoteMapper = DetailedNoteMapper(audioMetadataProvider)
        getDetailedNote = GetDetailedNote(repository, detailedNoteMapper)
    }

    @Test
    fun `When valid ID is provided, returns correctly mapped DetailedNote`() = runTest {
        val noteId = 1L
        val fakeNote = Note(
            id = noteId,
            title = "Test Note",
            content = "[ ] Task 1",
            audioUris = listOf("file://audio.mp3"),
            imageUris = emptyList(),
            timestamp = 12345L,
            color = 0xFFFFFF,
        )

        coEvery { repository.getNoteById(noteId) } returns NoteWithTags(fakeNote, tags = listOf( Tag("tag")))
        coEvery { audioMetadataProvider.getDuration(any()) } returns 3500L

        val result = getDetailedNote(noteId)

        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo("Test Note")
        assertThat(result?.isCheckboxListAvailable).isTrue()
        assertThat(result?.audioAttachments?.first()?.duration).isEqualTo(3500L)
        assertThat(result?.tags?.get(0)).isEqualTo("tag")
    }

    @Test
    fun `When note ID does not exist, returns null`() = runTest {
        val noteId = 999L
        coEvery { repository.getNoteById(noteId) } returns null

        val result = getDetailedNote(noteId)

        assertThat(result).isNull()
        coVerify(exactly = 0) { audioMetadataProvider.getDuration(any()) }
    }

    @Test
    fun `Verify Mapper Cache is utilized for multiple calls with same URI`() = runTest {
        val noteId = 1L
        val uri = "file://audio.mp3"
        val fakeNote = Note(
            id = noteId,
            title = "Title",
            content = "...",
            audioUris = listOf(uri),
            imageUris = emptyList(),
            timestamp = 0,
            color = 0
        )

        coEvery { repository.getNoteById(noteId) } returns NoteWithTags(fakeNote)
        coEvery { audioMetadataProvider.getDuration(any()) } returns 1000L

        getDetailedNote(noteId)
        getDetailedNote(noteId)

        coVerify(exactly = 1) { audioMetadataProvider.getDuration(uri.toUri()) }
    }
}