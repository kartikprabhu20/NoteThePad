package com.mintanable.notethepad.database.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteTagCrossRef
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("""
        SELECT * FROM noteEntity 
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
    @Query("SELECT * FROM noteEntity WHERE id =:id")
    suspend fun getNoteById(id: Long): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM noteEntity WHERE reminderTime > :currentTime")
    suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags>

    @Transaction
    @Query("SELECT * FROM noteEntity ORDER BY timestamp DESC LIMIT :limit")
    fun getTopNotes(limit: Int): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
    SELECT * FROM noteEntity
    WHERE id IN (
        SELECT noteId FROM note_tag_cross_ref 
        WHERE tagId = :tagId
    )
    """)
    fun getNotesByTag(tagId: Long): Flow<List<NoteWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserNote(noteEntity: NoteEntity): Long

    @Update
    suspend fun updateNote(noteEntity: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Delete
    suspend fun deleteNote(noteEntity: NoteEntity)

    @Query("DELETE FROM noteEntity WHERE id = :id")
    suspend fun deleteNoteWithId(id: Long)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteLinksForNote(noteId: Long)
}
