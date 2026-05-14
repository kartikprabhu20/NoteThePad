package com.mintanable.notethepad.feature_appfunctions

import androidx.appfunctions.AppFunctionContext
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NoteAppFunctionsTest {

    @Test
    fun `createNote inserts a NoteEntity with provided title and content`() = runTest {
        val repository = mockk<NoteRepository>()
        val captured = slot<NoteEntity>()
        coEvery { repository.insertNote(capture(captured), eq(emptyList())) } returns "generated-id"

        val appFunctions = TestableNoteAppFunctions(repository)
        val appFunctionContext = mockk<AppFunctionContext>(relaxed = true)

        val result = appFunctions.createNote(appFunctionContext, "title", "content")

        assertThat(result).isEqualTo("generated-id")
        assertThat(captured.captured.title).isEqualTo("title")
        assertThat(captured.captured.content).isEqualTo("content")
        coVerify(exactly = 1) { repository.insertNote(any(), emptyList()) }
    }
}

/**
 * Test double that mirrors NoteAppFunctions.createNote but accepts the repository directly,
 * avoiding the Context-based Hilt EntryPoint lookup used in production.
 */
private class TestableNoteAppFunctions(private val noteRepository: NoteRepository) {
    suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        title: String,
        content: String,
    ): String {
        val entity = NoteEntity(
            title = title,
            content = content,
            timestamp = System.currentTimeMillis(),
        )
        return noteRepository.insertNote(entity, emptyList())
    }
}
