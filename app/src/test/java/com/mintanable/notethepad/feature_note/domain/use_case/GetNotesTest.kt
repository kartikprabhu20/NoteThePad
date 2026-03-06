package com.mintanable.notethepad.feature_note.domain.use_case

import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.feature_note.domain.util.NoteOrder
import com.mintanable.notethepad.feature_note.domain.util.OrderType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class GetNotesTest {
    private lateinit var getNotes: GetNotes
    private val fakeRepository = mockk<NoteRepository>()

    @Before
    fun setUp() {
        getNotes = GetNotes(fakeRepository)
    }

    @Test
    fun `Sort notes by title ascending, returns correct order`(): Unit = runBlocking {

        val mockNotes = listOf(
            Note(title = "Apple", content = "...", timestamp = 1, color = 1),
            Note(title = "Banana", content = "...", timestamp = 2, color = 2),
            Note(title = "Cherry", content = "...", timestamp = 3, color = 3)
        )

        coEvery { fakeRepository.getNotes(NoteOrder.Title(OrderType.Ascending)) } returns flowOf(
            mockNotes
        )

        val result = getNotes(NoteOrder.Title(OrderType.Ascending)).first()

        assertThat(result[0].title).isEqualTo("Apple")
        assertThat(result[1].title).isEqualTo("Banana")
        assertThat(result[2].title).isEqualTo("Cherry")
    }
}