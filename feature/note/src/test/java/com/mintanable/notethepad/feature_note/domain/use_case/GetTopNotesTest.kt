package com.mintanable.notethepad.feature_note.domain.use_case

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.TestDispatcherProvider
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import com.mintanable.notethepad.feature_note.domain.use_case.notes.GetTopNotes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetTopNotesTest {

    private lateinit var getTopNotes: GetTopNotes
    private lateinit var mapper: DetailedNoteMapper

    private val repository = mockk<NoteRepository>()
    private val audioProvider = mockk<AudioMetadataProvider>()
    private val testDispatcherProvider = TestDispatcherProvider()

    @Before
    fun setUp() {
        mapper = DetailedNoteMapper(audioProvider, testDispatcherProvider)
        getTopNotes = GetTopNotes(repository, mapper)
    }

    @Test
    fun `When invoked, returns mapped detailed notes from repository`() = runTest {
        val limit = 2
        val mockNotes = listOf(
            NoteWithTags(NoteEntity(
                id = 1,
                title = "Top 1",
                content = "...",
                audioUris = emptyList(),
                timestamp = 0,
                color = 0
            ),
                tagEntities = listOf(TagEntity("test"))
            ),
            NoteWithTags(NoteEntity(
                id = 2,
                title = "Top 2",
                content = "...",
                audioUris = emptyList(),
                timestamp = 0,
                color = 0
            ))
        )

        every { repository.getTopNotes(limit) } returns flowOf(mockNotes)

        getTopNotes(limit).test {
            val result = awaitItem()
            assertThat(result).hasSize(2)
            assertThat(result[0].title).isEqualTo("Top 1")
            assertThat(result[0].tagEntities.size).isEqualTo(1)
            awaitComplete()
        }
    }
}