package com.mintanable.notethepad.database.db.repository

import com.mintanable.notethepad.core.model.collaboration.Collaborator
import com.mintanable.notethepad.core.network.sync.CollaborationService
import com.mintanable.notethepad.core.network.sync.NoteCollaboratorDto
import com.mintanable.notethepad.database.db.dao.CollaboratorDao
import com.mintanable.notethepad.database.db.entity.CollaboratorEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollaborationRepositoryImpl @Inject constructor(
    private val collaborationService: CollaborationService,
    private val collaboratorDao: CollaboratorDao
) : CollaborationRepository {

    override suspend fun findUserByEmail(email: String): Collaborator? {
        val result = collaborationService.findUserByEmail(email) ?: return null
        return Collaborator(
            id = "",
            noteId = "",
            userId = result.userId,
            email = email,
            displayName = result.displayName,
            photoUrl = result.photoUrl
        )
    }

    override suspend fun addCollaborator(
        noteId: String,
        ownerUserId: String,
        collaborator: Collaborator
    ): Boolean {
        val dto = NoteCollaboratorDto(
            noteId = noteId,
            ownerUserId = ownerUserId,
            collaboratorUserId = collaborator.userId,
            collaboratorEmail = collaborator.email,
            collaboratorDisplayName = collaborator.displayName,
            collaboratorPhotoUrl = collaborator.photoUrl
        )
        val success = collaborationService.addCollaborator(dto)
        if (success) {
            fetchAndCacheCollaborators(noteId)
        }
        return success
    }

    override suspend fun removeCollaborator(noteId: String, collaboratorUserId: String): Boolean {
        val success = collaborationService.removeCollaborator(noteId, collaboratorUserId)
        if (success) {
            collaboratorDao.deleteCollaborator(noteId, collaboratorUserId)
        }
        return success
    }

    override suspend fun leaveNote(noteId: String, userId: String): Boolean {
        return removeCollaborator(noteId, userId)
    }

    override fun getCollaboratorsForNote(noteId: String): Flow<List<CollaboratorEntity>> {
        return collaboratorDao.getCollaboratorsForNote(noteId)
    }

    override suspend fun fetchAndCacheCollaborators(noteId: String): List<CollaboratorEntity> {
        val remote = collaborationService.getCollaborators(noteId)
        val entities = remote.map { it.toEntity() }
        collaboratorDao.replaceCollaboratorsForNote(noteId, entities)
        return entities
    }

    override fun getSharedNoteIds(userId: String): Flow<List<String>> {
        return collaboratorDao.getSharedNoteIds(userId)
    }

    private fun NoteCollaboratorDto.toEntity() = CollaboratorEntity(
        id = id,
        noteId = noteId,
        ownerUserId = ownerUserId,
        collaboratorUserId = collaboratorUserId,
        collaboratorEmail = collaboratorEmail,
        collaboratorDisplayName = collaboratorDisplayName,
        collaboratorPhotoUrl = collaboratorPhotoUrl
    )
}
