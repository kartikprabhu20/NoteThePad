package com.mintanable.notethepad.database.db.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.network.sync.CollaborationService
import com.mintanable.notethepad.core.network.sync.SupabaseSyncService
import com.mintanable.notethepad.database.db.DatabaseManager
import com.mintanable.notethepad.database.db.util.toDto
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SupaSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val dbManager: DatabaseManager,
    private val supabaseSyncService: SupabaseSyncService,
    private val collaborationService: CollaborationService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SupaSyncWorker", "Starting sync")

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

            // Ensure Auth is valid
            supabaseSyncService.ensureAuthenticated(authRepository.getFreshFirebaseToken())

            val noteDao = dbManager.database.noteDao()
            val tagDao = dbManager.database.tagDao()
            val collaboratorDao = dbManager.database.collaboratorDao()

            //Fetch all unsynced notes (Active & Deleted)
            val unsyncedNotes = noteDao.getUnsyncedNotes()
            val unsyncedDeletedNotes = noteDao.getUnsyncedDeletedNotes()

            //Fetch all unsynced tags (Active & Deleted)
            val unsyncedTags = tagDao.getUnsyncedTags()
            val unsyncedDeletedTags = tagDao.getUnsyncedDeletedTags()

            var allSuccessful = true

            Log.d("kptest", "unsyncedNotes size: ${unsyncedNotes.size}")
            Log.d("kptest", "unsyncedDeletedNotes size: ${unsyncedDeletedNotes.size}")
            Log.d("kptest", "unsyncedTags size: ${unsyncedTags.size}")
            Log.d("kptest", "unsyncedDeletedTags size: ${unsyncedDeletedTags.size}")

            // Sync Notes
            (unsyncedNotes + unsyncedDeletedNotes).forEach { noteWithTags ->
                val note = noteWithTags.noteEntity
                if (note.userId != null) {
                    // Use collaborator table to determine shared status — userId field
                    // may have been corrupted to the collaborator's ID by a prior save.
                    val collabs = collaboratorDao.getCollaboratorsForNoteOnce(note.id)
                    val ownerFromCollab = collabs.firstOrNull()?.ownerUserId
                    val isSharedNote = ownerFromCollab != null && ownerFromCollab != userId
                    Log.d("kptest", "Syncing note ${note.id}, userId=${note.userId}, currentUser=$userId, isShared=$isSharedNote, owner=$ownerFromCollab")
                    val success = if (isSharedNote) {
                        // updateSharedNote strips user_id from the payload
                        supabaseSyncService.updateSharedNote(note.toDto())
                    } else {
                        supabaseSyncService.syncNote(note.toDto())
                    }
                    if (success) {
                        // Sync associated cross-refs to ensure linkages are uploaded
                        val crossRefs = noteDao.getCrossRefsForNote(note.id)
                        var crossRefsSuccess = true
                        for (crossRef in crossRefs) {
                            if (!supabaseSyncService.syncCrossRef(crossRef.toDto())) {
                                crossRefsSuccess = false
                            }
                        }

                        if (crossRefsSuccess) {
                            noteDao.updateNote(note.copy(isSynced = true))
                            Log.d("SupaSyncWorker", "Synced Note & CrossRefs: ${note.id}")
                        } else {
                            allSuccessful = false
                        }
                    } else {
                        allSuccessful = false
                    }
                }
            }

            // Sync Tags
            (unsyncedTags + unsyncedDeletedTags).forEach { tag ->
                if (tag.userId != null) {
                    val success = supabaseSyncService.syncTag(tag.toDto())
                    if (success) {
                        tagDao.updateTag(tag.copy(isSynced = true))
                        Log.d("SupaSyncWorker", "Synced Tag: ${tag.tagId}")
                    } else {
                        allSuccessful = false
                    }
                }
            }

            // Upsert current user profile
            val userEmail = user.email
            if (userEmail != null) {
                val isUserUploaded = collaborationService.upsertUserProfile(
                    userId = userId,
                    email = userEmail,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl
                )
                if(isUserUploaded){
                    Log.d("SupaSyncWorker", "Synced userprofile")
                } else {
                    allSuccessful = false
                }
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
