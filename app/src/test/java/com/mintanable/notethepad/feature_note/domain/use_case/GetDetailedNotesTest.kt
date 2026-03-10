package com.mintanable.notethepad.feature_note.domain.use_case

import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteWithTags
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.DetailedNoteMapper
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetDetailedNotesTest {
    private lateinit var getDetailedNotes: GetDetailedNotes
    private lateinit var detailedNoteMapper: DetailedNoteMapper

    // Mocks
    private val repository = mockk<NoteRepository>()
    private val audioMetadataProvider = mockk<AudioMetadataProvider>()

    @Before
    fun setUp() {
        detailedNoteMapper = DetailedNoteMapper(audioMetadataProvider)
        getDetailedNotes = GetDetailedNotes(repository, detailedNoteMapper)
    }

    @Test
    fun `Sort notes by title ascending, returns correct order with mapped details`() = runTest {

        val mockNotes = listOf(
            NoteWithTags(
                Note(id = 1, title = "Apple", content = "[ ] Buy Apple", audioUris = listOf("uri1"), timestamp = 1, color = 1),
                tags = listOf(Tag("fruits"))
            ),
            NoteWithTags(Note(id = 2, title = "Banana", content = "Just a fruit", audioUris = emptyList(), timestamp = 2, color = 2))
        )

        coEvery { repository.getNotes(any()) } returns flowOf(mockNotes)
        coEvery { audioMetadataProvider.getDuration(any()) } returns 5000L

        val result = getDetailedNotes(NoteOrder.Title(OrderType.Ascending)).first()

        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("Apple")
        assertThat(result[0].tags[0]).isEqualTo(Tag("fruits"))

        assertThat(result[0].isCheckboxListAvailable).isTrue()
        assertThat(result[0].checkListItems.first().text).isEqualTo("Buy Apple")

        assertThat(result[0].audioAttachments.first().duration).isEqualTo(5000L)
    }

    @Test
    fun `Verify LruCache prevents redundant audio metadata calls`() = runTest {
        val uri = "file://audio.mp3"
        val note = Note(id = 1, title = "Test", content = "...", audioUris = listOf(uri), timestamp = 1, color = 1)

        coEvery { repository.getNotes(any()) } returns flowOf(listOf(NoteWithTags(note)), listOf(NoteWithTags(note)))
        coEvery { audioMetadataProvider.getDuration(any()) } returns 3000L

        getDetailedNotes(NoteOrder.Date(OrderType.Ascending)).take(2).collect { }

        coVerify(exactly = 1) { audioMetadataProvider.getDuration(uri.toUri()) }
    }
}