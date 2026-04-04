package com.mintanable.notethepad.database.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mintanable.notethepad.database.db.entity.CollaboratorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollaboratorDao {

    @Query("SELECT * FROM note_collaborators_local WHERE noteId = :noteId")
    fun getCollaboratorsForNote(noteId: String): Flow<List<CollaboratorEntity>>

    @Query("SELECT * FROM note_collaborators_local WHERE noteId = :noteId")
    suspend fun getCollaboratorsForNoteOnce(noteId: String): List<CollaboratorEntity>

    @Query("SELECT * FROM note_collaborators_local WHERE collaboratorUserId = :userId")
    fun getSharedWithMe(userId: String): Flow<List<CollaboratorEntity>>

    @Query("SELECT * FROM note_collaborators_local WHERE ownerUserId = :userId")
    fun getSharedByMe(userId: String): Flow<List<CollaboratorEntity>>

    @Query("SELECT DISTINCT noteId FROM note_collaborators_local WHERE collaboratorUserId = :userId OR ownerUserId = :userId")
    fun getSharedNoteIds(userId: String): Flow<List<String>>

    @Query("SELECT DISTINCT noteId FROM note_collaborators_local WHERE collaboratorUserId = :userId OR ownerUserId = :userId")
    suspend fun getSharedNoteIdsOnce(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollaborator(entity: CollaboratorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CollaboratorEntity>)

    @Query("DELETE FROM note_collaborators_local WHERE noteId = :noteId AND collaboratorUserId = :collaboratorUserId")
    suspend fun deleteCollaborator(noteId: String, collaboratorUserId: String)

    @Query("DELETE FROM note_collaborators_local WHERE noteId = :noteId")
    suspend fun deleteAllForNote(noteId: String)

    @Transaction
    suspend fun replaceCollaboratorsForNote(noteId: String, entities: List<CollaboratorEntity>) {
        deleteAllForNote(noteId)
        insertAll(entities)
    }

    @Query("DELETE FROM note_collaborators_local")
    suspend fun deleteAll()
}
