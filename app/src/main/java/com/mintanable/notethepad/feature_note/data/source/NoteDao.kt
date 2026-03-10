package com.mintanable.notethepad.feature_note.data.source

import androidx.room.*
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteTagCrossRef
import com.mintanable.notethepad.feature_note.domain.model.NoteWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("""
        SELECT * FROM note 
        ORDER BY 
            CASE WHEN :order = 'title' AND :ascending = 1 THEN title END ASC,
            CASE WHEN :order = 'title' AND :ascending = 0 THEN title END DESC,
            CASE WHEN :order = 'date' AND :ascending = 1 THEN timestamp END ASC,
            CASE WHEN :order = 'date' AND :ascending = 0 THEN timestamp END DESC,
            CASE WHEN :order = 'color' AND :ascending = 1 THEN color END ASC,
            CASE WHEN :order = 'color' AND :ascending = 0 THEN color END DESC
    """)
    fun getNotes(order: String, ascending: Int): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM note WHERE id =:id")
    suspend fun getNoteById(id:Long):NoteWithTags?

    @Transaction
    @Query("SELECT * FROM note WHERE reminderTime > :currentTime")
    suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags>

    @Transaction
    @Query("SELECT * FROM note ORDER BY timestamp DESC LIMIT :limit")
    fun getTopNotes(limit: Int):  Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
    SELECT * FROM note
    WHERE id IN (
        SELECT noteId FROM note_tag_cross_ref 
        WHERE tagId = :tagId
    )
    """)
    fun getNotesByTag(tagId: Long): Flow<List<NoteWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Delete
    suspend fun deleteNote(note:Note)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteNoteWithId(id:Long)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteLinksForNote(noteId: Long)
}