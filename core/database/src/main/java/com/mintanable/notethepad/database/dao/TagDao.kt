package com.mintanable.notethepad.database.dao

import androidx.room.*
import com.mintanable.notethepad.core.model.Tag
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
