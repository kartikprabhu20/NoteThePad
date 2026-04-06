package com.mintanable.notethepad.database.db.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.common.FeatureFlags
import com.mintanable.notethepad.core.network.sync.CollaborationService
import com.mintanable.notethepad.core.network.sync.SupabaseSyncService
import com.mintanable.notethepad.database.db.DatabaseManager
import com.mintanable.notethepad.database.db.entity.CollaboratorEntity
import com.mintanable.notethepad.database.db.util.toEntity
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SupaFetchWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val dbManager: DatabaseManager,
    private val supabaseSyncService: SupabaseSyncService,
    private val collaborationService: CollaborationService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SupaFetchWorker", "Starting fetch")

        return try {

            val settings = userPreferencesRepository.settingsFlow.first()
            if (!settings.supaSyncEnabled) {
                Log.e("SupaFetchWorker", "Supa sync disabled - EXITING EARLY")
                return Result.success()
            }

            val user = authRepository.getSignedInFirebaseUser().first()
            if (user == null) {
                Log.e("SupaFetchWorker", "USER IS NULL - EXITING EARLY")
                return Result.failure()
            }
            val userId = user.uid

            val noteDao = dbManager.database.noteDao()
            val tagDao = dbManager.database.tagDao()
            val db = dbManager.database

            // Ensure Auth is valid
            supabaseSyncService.ensureAuthenticated(authRepository.getFreshFirebaseToken())


            var allSuccessful = true
            supabaseSyncService.setSyncing(true)
            try {
                val remoteNotes = supabaseSyncService.fetchNotes(userId)
                val remoteTags = supabaseSyncService.fetchTags(userId).toMutableList()
                val remoteRefs = supabaseSyncService.fetchCrossRefs(userId).toMutableList()

                Log.d("kptest", "remoteNotes size: ${remoteNotes.size}")
                Log.d("kptest", "remoteTags size: ${remoteTags.size}")
                Log.d("kptest", "remoteRefs size: ${remoteRefs.size}")

                db.withTransaction {
                    remoteNotes.forEach { dto ->
                        val local = noteDao.getNoteById(dto.id)
                        if (local == null || dto.lastUpdateTime > local.noteEntity.lastUpdateTime) {
                            val rowid = noteDao.inserNote(dto.toEntity())
                            if(rowid == -1L)
                                noteDao.updateNote(dto.toEntity())
                        }
                    }
                    remoteTags.forEach { dto ->
                        val local = tagDao.getTagById(dto.tagId)
                        if (local == null || dto.lastUpdateTime > local.lastUpdateTime) {
                            val rowid = tagDao.insertTag(dto.toEntity())
                            if(rowid == -1L)
                                tagDao.updateTag(dto.toEntity())
                        }
                    }
                    remoteRefs.forEach { dto ->
                        val noteExists = noteDao.getNoteById(dto.noteId) != null
                        val tagExists = tagDao.getTagById(dto.tagId) != null
                        if (noteExists && tagExists) {
                            val localRefs = noteDao.getCrossRefsForNote(dto.noteId)
                            val localRef = localRefs.find { it.tagId == dto.tagId }
                            // Only apply remote if it's newer than local
                            if (localRef == null || dto.lastUpdateTime > localRef.lastUpdateTime) {
                                noteDao.insertNoteTagCrossRef(dto.toEntity())
                            }
                        }
                    }
                }

                if (FeatureFlags.collaborationEnabled) {
                    // Fetch shared notes (notes others shared with this user)
                    val sharedNotes = collaborationService.fetchSharedNotes(userId)
                    Log.d("kptest", "sharedNotes size: ${sharedNotes.size}")
                    val sharedNoteIds = sharedNotes.map { it.id }

                    // Fetch tags and cross-refs for shared notes (owned by others)
                    val sharedCrossRefs = supabaseSyncService.fetchCrossRefsForNotes(sharedNoteIds)
                    val sharedTagIds = sharedCrossRefs.map { it.tagId }.distinct()
                    val sharedTags = supabaseSyncService.fetchTagsByIds(sharedTagIds)
                    Log.d("kptest", "sharedTags size: ${sharedTags.size}, sharedCrossRefs size: ${sharedCrossRefs.size}")

                    db.withTransaction {
                        sharedNotes.forEach { dto ->
                            val local = noteDao.getNoteById(dto.id)
                            if (local == null || dto.lastUpdateTime > local.noteEntity.lastUpdateTime) {
                                val rowid = noteDao.inserNote(dto.toEntity())
                                if (rowid == -1L)
                                    noteDao.updateNote(dto.toEntity())
                            }
                        }

                        // Insert shared tags
                        sharedTags.forEach { dto ->
                            val local = tagDao.getTagById(dto.tagId)
                            if (local == null || dto.lastUpdateTime > local.lastUpdateTime) {
                                val rowid = tagDao.insertTag(dto.toEntity())
                                if (rowid == -1L)
                                    tagDao.updateTag(dto.toEntity())
                            }
                        }

                        // Insert shared cross-refs
                        sharedCrossRefs.forEach { dto ->
                            val noteExists = noteDao.getNoteById(dto.noteId) != null
                            val tagExists = tagDao.getTagById(dto.tagId) != null
                            if (noteExists && tagExists) {
                                val localRefs = noteDao.getCrossRefsForNote(dto.noteId)
                                val localRef = localRefs.find { it.tagId == dto.tagId }
                                if (localRef == null || dto.lastUpdateTime > localRef.lastUpdateTime) {
                                    noteDao.insertNoteTagCrossRef(dto.toEntity())
                                }
                            }
                        }
                    }

                    // Sync collaborator records to local cache
                    val collaboratorDao = dbManager.database.collaboratorDao()

                    // Snapshot current shared note IDs before replacing cache
                    val oldSharedNoteIds = collaboratorDao.getSharedNoteIdsOnce(userId).toSet()

                    val myCollaborations = collaborationService.getMyCollaborations(userId)
                    val notesIShared = collaborationService.getNotesIShared(userId)
                    val allCollaboratorDtos = (myCollaborations + notesIShared).distinctBy { it.id }
                    val collaboratorEntities = allCollaboratorDtos.filter { it.id != null }.map { dto ->
                        CollaboratorEntity(
                            id = dto.id!!,
                            noteId = dto.noteId,
                            ownerUserId = dto.ownerUserId,
                            collaboratorUserId = dto.collaboratorUserId,
                            collaboratorEmail = dto.collaboratorEmail,
                            collaboratorDisplayName = dto.collaboratorDisplayName,
                            collaboratorPhotoUrl = dto.collaboratorPhotoUrl
                        )
                    }
                    db.withTransaction {
                        collaboratorDao.deleteAll()
                        collaboratorDao.insertAll(collaboratorEntities)

                        // Duplicate orphaned notes whose collaboration was severed
                        val newSharedNoteIds = collaboratorEntities
                            .filter { it.collaboratorUserId == userId || it.ownerUserId == userId }
                            .map { it.noteId }
                            .toSet()
                        val removedNoteIds = oldSharedNoteIds - newSharedNoteIds
                        for (noteId in removedNoteIds) {
                            val local = noteDao.getNoteById(noteId)
                            if (local != null && local.noteEntity.userId != userId) {
                                val duplicated = local.noteEntity.copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    userId = userId,
                                    lastUpdateTime = System.currentTimeMillis(),
                                    isSynced = false
                                )
                                noteDao.inserNote(duplicated)
                                noteDao.deleteNoteWithId(noteId)
                                noteDao.deleteLinksForNote(noteId)
                            }
                        }
                    }
                    Log.d("kptest", "collaborators synced: ${collaboratorEntities.size}")
                }

            } catch (e: Exception) {
                Log.e("Sync", "Pull failed: ${e.message}")
                allSuccessful = false
            } finally {
                supabaseSyncService.setSyncing(false)
            }

            if (allSuccessful) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SupaFetchWorker", "Fetch failed: ${e.message}")
            Result.retry()
        }
    }
}
