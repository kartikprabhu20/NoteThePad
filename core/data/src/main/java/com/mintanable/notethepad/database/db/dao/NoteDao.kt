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
        WHERE isDeleted = 0
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
    suspend fun getNoteById(id: String): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM noteEntity WHERE reminderTime > :currentTime AND isDeleted = 0")
    suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags>

    @Transaction
    @Query("SELECT * FROM noteEntity WHERE isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getTopNotes(limit: Int): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
    SELECT * FROM noteEntity
    WHERE isDeleted = 0 AND id IN (
        SELECT noteId FROM note_tag_cross_ref 
        WHERE tagId = :tagId AND isDeleted = 0
    )
    """)
    fun getNotesByTag(tagId: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM noteEntity WHERE id IN (:ids) AND isDeleted = 0")
    fun getNotesByIds(ids: List<String>): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM noteEntity WHERE isDeleted = 1 ORDER BY lastUpdateTime DESC")
    fun getDeletedNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM noteEntity WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedNotes(): List<NoteWithTags>

    @Transaction
    @Query("SELECT * FROM noteEntity WHERE isSynced = 0 AND isDeleted = 1")
    suspend fun getUnsyncedDeletedNotes(): List<NoteWithTags>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserNote(noteEntity: NoteEntity): Long

    @Update
    suspend fun updateNote(noteEntity: NoteEntity)

    @Query("SELECT * FROM noteEntity WHERE imageUris != '' OR audioUris != ''")
    suspend fun getNotesWithMedia(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Delete
    suspend fun deleteNote(noteEntity: NoteEntity)

    @Query("DELETE FROM noteEntity WHERE id = :id")
    suspend fun deleteNoteWithId(id: String)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteLinksForNote(noteId: String)

    @Query("SELECT * FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun getCrossRefsForNote(noteId: String): List<NoteTagCrossRef>

    @Query("SELECT tagId FROM note_tag_cross_ref WHERE noteId = :noteId AND isDeleted = 0")
    suspend fun getActiveTagIdsForNote(noteId: String): List<String>

    @Query("UPDATE NoteEntity SET summary = :summary, lastUpdateTime = :lastUpdateTime WHERE id = :noteId")
    suspend fun updateSummary(noteId: String, summary: String, lastUpdateTime: Long)

    @Query("UPDATE NoteEntity SET reminderTime = :reminderTime, lastUpdateTime = :lastUpdateTime WHERE id = :noteId")
    suspend fun updateReminderTime(noteId: String, reminderTime: Long, lastUpdateTime: Long)
}
