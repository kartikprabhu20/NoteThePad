package com.mintanable.notethepad.database.db.repository

import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.model.collaboration.Collaborator
import com.mintanable.notethepad.core.network.sync.CollaborationService
import com.mintanable.notethepad.core.network.sync.NoteCollaboratorDto
import com.mintanable.notethepad.database.db.DatabaseManager
import com.mintanable.notethepad.database.db.dao.CollaboratorDao
import com.mintanable.notethepad.database.db.entity.CollaboratorEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollaborationRepositoryImpl @Inject constructor(
    private val collaborationService: CollaborationService,
    private val collaboratorDao: CollaboratorDao,
    private val authRepository: AuthRepository,
    private val dbManager: DatabaseManager
) : CollaborationRepository {

    private suspend fun ensureAuth() {
        collaborationService.ensureAuthenticated(authRepository.getFreshFirebaseToken())
    }

    override suspend fun findUserByEmail(email: String): Collaborator? {
        ensureAuth()
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
        ensureAuth()
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
        ensureAuth()
        val success = collaborationService.removeCollaborator(noteId, collaboratorUserId)
        if (success) {
            collaboratorDao.deleteCollaborator(noteId, collaboratorUserId)
        }
        return success
    }

    override suspend fun leaveNote(noteId: String, userId: String): Boolean {
        val noteDao = dbManager.database.noteDao()
        val tagDao = dbManager.database.tagDao()
        val local = noteDao.getNoteById(noteId)
        if (local != null && local.noteEntity.userId != userId) {
            val duplicated = local.noteEntity.copy(
                id = UUID.randomUUID().toString(),
                userId = userId,
                lastUpdateTime = System.currentTimeMillis(),
                isSynced = false
            )
            noteDao.inserNote(duplicated)

            val existingRefs = noteDao.getCrossRefsForNote(noteId)
            existingRefs.filter { !it.isDeleted }.forEach { ref ->
                val tag = tagDao.getTagById(ref.tagId)
                val newTagId = if (tag != null && tag.userId != userId) {
                    val userTag = tagDao.getTagByNameAndUserId(tag.tagName, userId)
                    if (userTag != null) {
                        userTag.tagId
                    } else {
                        val copiedTag = tag.copy(
                            tagId = UUID.randomUUID().toString(),
                            userId = userId,
                            lastUpdateTime = System.currentTimeMillis(),
                            isSynced = false
                        )
                        tagDao.insertTag(copiedTag)
                        copiedTag.tagId
                    }
                } else {
                    ref.tagId
                }
                noteDao.insertNoteTagCrossRef(
                    ref.copy(
                        noteId = duplicated.id,
                        tagId = newTagId,
                        userId = userId,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                )
            }

            noteDao.deleteNoteWithId(noteId)
            noteDao.deleteLinksForNote(noteId)
        }
        return removeCollaborator(noteId, userId)
    }

    override fun getCollaboratorsForNote(noteId: String): Flow<List<CollaboratorEntity>> {
        return collaboratorDao.getCollaboratorsForNote(noteId)
    }

    override suspend fun fetchAndCacheCollaborators(noteId: String): List<CollaboratorEntity> {
        ensureAuth()
        val remote = collaborationService.getCollaborators(noteId)
        val entities = remote.filter { it.id != null }.map { it.toEntity() }
        collaboratorDao.replaceCollaboratorsForNote(noteId, entities)
        return entities
    }

    override fun getSharedNoteIds(userId: String): Flow<List<String>> {
        return collaboratorDao.getSharedNoteIds(userId)
    }

    private fun NoteCollaboratorDto.toEntity() = CollaboratorEntity(
        id = id!!,
        noteId = noteId,
        ownerUserId = ownerUserId,
        collaboratorUserId = collaboratorUserId,
        collaboratorEmail = collaboratorEmail,
        collaboratorDisplayName = collaboratorDisplayName,
        collaboratorPhotoUrl = collaboratorPhotoUrl
    )
}
