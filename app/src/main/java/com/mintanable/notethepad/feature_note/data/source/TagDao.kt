package com.mintanable.notethepad.feature_note.data.source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mintanable.notethepad.feature_note.domain.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Query("SELECT * FROM tag_table")
    fun getAllTags(): Flow<List<Tag>>

    @Delete
    suspend fun deleteTag(tag: Tag)
}