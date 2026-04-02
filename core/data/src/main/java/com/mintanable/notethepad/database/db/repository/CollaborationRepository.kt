package com.mintanable.notethepad.database.db.repository

import com.mintanable.notethepad.core.model.collaboration.Collaborator
import com.mintanable.notethepad.database.db.entity.CollaboratorEntity
import kotlinx.coroutines.flow.Flow

interface CollaborationRepository {
    suspend fun findUserByEmail(email: String): Collaborator?
    suspend fun addCollaborator(noteId: String, ownerUserId: String, collaborator: Collaborator): Boolean
    suspend fun removeCollaborator(noteId: String, collaboratorUserId: String): Boolean
    suspend fun leaveNote(noteId: String, userId: String): Boolean
    fun getCollaboratorsForNote(noteId: String): Flow<List<CollaboratorEntity>>
    suspend fun fetchAndCacheCollaborators(noteId: String): List<CollaboratorEntity>
    fun getSharedNoteIds(userId: String): Flow<List<String>>
}
