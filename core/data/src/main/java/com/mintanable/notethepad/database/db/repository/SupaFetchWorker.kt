package com.mintanable.notethepad.database.db.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mintanable.notethepad.auth.repository.AuthRepository
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
            val noteDao = dbManager.database.noteDao()
            val tagDao = dbManager.database.tagDao()
            val db = dbManager.database

            val user = authRepository.getSignedInFirebaseUser().first()
            val userId = user?.uid ?: return Result.retry()

            var allSuccessful = true
            supabaseSyncService.setSyncing(true)
            try {
                val remoteNotes = supabaseSyncService.fetchNotes(userId)
                val remoteTags = supabaseSyncService.fetchTags(userId)
                val remoteRefs = supabaseSyncService.fetchCrossRefs(userId)

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
                        noteDao.insertNoteTagCrossRef(dto.toEntity())
                    }
                }

                // Fetch shared notes (notes others shared with this user)
                val sharedNotes = collaborationService.fetchSharedNotes(userId)
                Log.d("kptest", "sharedNotes size: ${sharedNotes.size}")
                db.withTransaction {
                    sharedNotes.forEach { dto ->
                        val local = noteDao.getNoteById(dto.id)
                        if (local == null || dto.lastUpdateTime > local.noteEntity.lastUpdateTime) {
                            val rowid = noteDao.inserNote(dto.toEntity())
                            if (rowid == -1L)
                                noteDao.updateNote(dto.toEntity())
                        }
                    }
                }

                // Sync collaborator records to local cache
                val collaboratorDao = dbManager.database.collaboratorDao()
                val myCollaborations = collaborationService.getMyCollaborations(userId)
                val notesIShared = collaborationService.getNotesIShared(userId)
                val allCollaboratorDtos = (myCollaborations + notesIShared).distinctBy { it.id }
                val collaboratorEntities = allCollaboratorDtos.map { dto ->
                    CollaboratorEntity(
                        id = dto.id,
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
                }
                Log.d("kptest", "collaborators synced: ${collaboratorEntities.size}")

                // Upsert current user profile
                val userEmail = user.email
                if (userEmail != null) {
                    collaborationService.upsertUserProfile(
                        userId = userId,
                        email = userEmail,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl
                    )
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
            Log.e("SupaSyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
