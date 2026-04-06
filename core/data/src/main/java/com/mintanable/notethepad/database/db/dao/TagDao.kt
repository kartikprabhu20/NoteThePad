package com.mintanable.notethepad.database.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mintanable.notethepad.database.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tag_table WHERE isDeleted = 0")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag_table WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTags(): List<TagEntity>

    @Query("SELECT * FROM tag_table WHERE isSynced = 0 AND isDeleted = 1")
    suspend fun getUnsyncedDeletedTags(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tagEntity: TagEntity): Long

    @Update
    suspend fun updateTag(tagEntity: TagEntity)

    @Delete
    suspend fun deleteTag(tagEntity: TagEntity)

    @Query("SELECT * FROM tag_table WHERE tagId = :id")
    suspend fun getTagById(id: String): TagEntity?

    @Query("SELECT * FROM tag_table WHERE tagName = :tagName AND isDeleted = 0")
    suspend fun getTagByName(tagName: String): TagEntity?

    @Query("SELECT * FROM tag_table WHERE tagName = :tagName")
    suspend fun getTagByNameIncludeDeleted(tagName: String): TagEntity?
}
