package com.mintanable.notethepad.database.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mintanable.notethepad.core.model.note.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tag_table")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM tag_table WHERE tagId = :id")
    suspend fun getTagById(id: Long): Tag?

    @Query("SELECT * FROM tag_table WHERE tagName = :tagName")
    suspend fun getTagByName(tagName: String): Tag?
}
