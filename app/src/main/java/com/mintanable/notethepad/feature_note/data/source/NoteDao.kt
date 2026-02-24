package com.mintanable.notethepad.feature_note.data.source

import androidx.room.*
import com.mintanable.notethepad.feature_note.domain.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

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
    fun getNotes(order: String, ascending: Int): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE id =:id")
    suspend fun getNoteById(id:Int):Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserNote(note: Note)

    @Delete
    suspend fun deleteNote(note:Note)
}