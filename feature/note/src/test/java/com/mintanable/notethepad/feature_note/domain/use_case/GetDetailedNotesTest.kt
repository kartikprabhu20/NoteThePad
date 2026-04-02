package com.mintanable.notethepad.feature_note.domain.use_case

import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.TestDispatcherProvider
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import com.mintanable.notethepad.feature_note.domain.use_case.notes.GetDetailedNotes
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
    private val testDispatcherProvider = TestDispatcherProvider()

    @Before
    fun setUp() {
        detailedNoteMapper = DetailedNoteMapper(audioMetadataProvider, testDispatcherProvider)
        getDetailedNotes = GetDetailedNotes(repository, detailedNoteMapper)
    }

    @Test
    fun `Sort notes by title ascending, returns correct order with mapped details`() = runTest {

        val tag = TagEntity("fruits")
        val mockNotes = listOf(
            NoteWithTags(
                NoteEntity(id = "1", title = "Apple", content = "[ ] Buy Apple", audioUris = listOf("uri1"), timestamp = 1, color = 1),
                tagEntities = listOf(tag)
            ),
            NoteWithTags(NoteEntity(id = "2", title = "Banana", content = "Just a fruit", audioUris = emptyList(), timestamp = 2, color = 2))
        )

        coEvery { repository.getNotes(any()) } returns flowOf(mockNotes)
        coEvery { audioMetadataProvider.getDuration(any()) } returns 5000L

        val result = getDetailedNotes(NoteOrder.Title(OrderType.Ascending)).first()

        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("Apple")
        assertThat(result[0].tagEntities[0]).isEqualTo(tag)

        assertThat(result[0].isCheckboxListAvailable).isTrue()
        assertThat(result[0].checkListItems.first().text).isEqualTo("Buy Apple")

        assertThat(result[0].audioAttachments.first().duration).isEqualTo(5000L)
    }

    @Test
    fun `Verify LruCache prevents redundant audio metadata calls`() = runTest {
        val uri = "file://audio.mp3"
        val noteEntity = NoteEntity(id = "1", title = "Test", content = "...", audioUris = listOf(uri), timestamp = 1, color = 1)

        coEvery { repository.getNotes(any()) } returns flowOf(listOf(NoteWithTags(noteEntity)), listOf(NoteWithTags(noteEntity)))
        coEvery { audioMetadataProvider.getDuration(any()) } returns 3000L

        getDetailedNotes(NoteOrder.Date(OrderType.Ascending)).take(2).collect { }

        coVerify(exactly = 1) { audioMetadataProvider.getDuration(uri.toUri()) }
    }
}