package com.mintanable.notethepad.database.db.repository

import android.util.Log
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.model.NoteThePadConstants.SUPA_FETCH_WORKER
import com.mintanable.notethepad.core.model.NoteThePadConstants.SUPA_SYNC_WORKER
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.core.network.sync.SupabaseSyncService
import com.mintanable.notethepad.database.db.DatabaseManager
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteTagCrossRef
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val dbManager: DatabaseManager,
    private val supabaseSyncService: SupabaseSyncService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val workManager: WorkManager
) : NoteRepository {

    override val isSyncing: Flow<Boolean> = supabaseSyncService.isSyncing

    // Helper properties to always get the LATEST instance
    private val noteDao get() = dbManager.database.noteDao()
    private val tagDao get() = dbManager.database.tagDao()
    private val db get() = dbManager.database

    override fun getNotes(noteOrder: NoteOrder): Flow<List<NoteWithTags>> {
        return noteDao.getNotes(
            order = when (noteOrder) {
                is NoteOrder.Title -> "title"
                is NoteOrder.Date -> "date"
                is NoteOrder.Color -> "color"
            },
            ascending = if (noteOrder.orderType == OrderType.Ascending) 1 else 0
        )
    }

    override suspend fun getNoteById(id: String): NoteWithTags? {
        return noteDao.getNoteById(id)
    }

    override suspend fun insertNote(noteEntity: NoteEntity, tagEntities: List<TagEntity>): String {
        return withContext(Dispatchers.IO) {
            val user = authRepository.getSignedInFirebaseUser().first()
            val userId = user?.uid

            val updatedNote = noteEntity.copy(
                userId = userId,
                lastUpdateTime = System.currentTimeMillis(),
                isDeleted = false,
                isSynced = false
            )

            db.withTransaction {
                val rowid = noteDao.inserNote(updatedNote)
                if(rowid == -1L) noteDao.updateNote(updatedNote) // Ensure update if IGNORE failed
                val noteId = updatedNote.id

                noteDao.deleteLinksForNote(noteId)
                tagEntities.forEach { tag ->
                    val updatedTag = tag.copy(
                        userId = userId,
                        lastUpdateTime = System.currentTimeMillis(),
                        isDeleted = false,
                        isSynced = false
                    )
                    val tagRowid = tagDao.insertTag(updatedTag)
                    if(tagRowid == -1L) tagDao.updateTag(updatedTag)
                    
                    val crossRef = NoteTagCrossRef(
                        noteId = noteId,
                        tagId = updatedTag.tagId,
                        userId = userId,
                        lastUpdateTime = System.currentTimeMillis(),
                        isDeleted = false
                    )
                    noteDao.insertNoteTagCrossRef(crossRef)
                }
                triggerSync()
                noteId
            }
        }
    }

    override suspend fun deleteNote(noteEntity: NoteEntity) {
        val deletedNote = noteEntity.copy(
            isDeleted = true,
            lastUpdateTime = System.currentTimeMillis(),
            isSynced = false
        )
        noteDao.updateNote(deletedNote)
        triggerSync()
    }

    override suspend fun deleteNoteWithId(id: String) {
        val noteWithTags = noteDao.getNoteById(id)
        noteWithTags?.noteEntity?.let { deleteNote(it) }
    }

    override suspend fun restoreNote(id: String) {
        val noteWithTags = noteDao.getNoteById(id)
        noteWithTags?.noteEntity?.let {
            val restoredNote = it.copy(
                isDeleted = false,
                lastUpdateTime = System.currentTimeMillis(),
                isSynced = false
            )
            noteDao.updateNote(restoredNote)
            triggerSync()
        }
    }

    override suspend fun deleteNotePermanently(id: String) {
        noteDao.deleteNoteWithId(id)
        noteDao.deleteLinksForNote(id)
      }

    override fun getDeletedNotes(): Flow<List<NoteWithTags>> {
        return noteDao.getDeletedNotes()
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SupaSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            SUPA_SYNC_WORKER,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun enqueueFetch() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SupaFetchWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            SUPA_FETCH_WORKER,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    override suspend fun getNotesWithFutureReminders(currentTime: Long): List<NoteWithTags> {
        return noteDao.getNotesWithFutureReminders(currentTime)
    }

    override fun getTopNotes(limit: Int): Flow<List<NoteWithTags>> {
        return noteDao.getTopNotes(limit)
    }

    override fun getAllTags(): Flow<List<TagEntity>> {
        return tagDao.getAllTags()
    }

    override suspend fun insertTag(tagEntity: TagEntity): Long {
        val tagToInsert = if (tagEntity.isSynced) tagEntity.copy(isSynced = false) else tagEntity
        val rowId = tagDao.insertTag(tagToInsert)
        triggerSync()
        return rowId
    }

    override suspend fun updateTag(tagEntity: TagEntity) {
        tagDao.updateTag(tagEntity.copy(isSynced = false))
        triggerSync()
    }

    override suspend fun deleteTag(tagEntity: TagEntity) {
        val deletedTag = tagEntity.copy(
            isDeleted = true,
            lastUpdateTime = System.currentTimeMillis(),
            isSynced = false
        )
        tagDao.updateTag(deletedTag)
        triggerSync()
    }

    override suspend fun getTagByName(tagName: String): TagEntity? {
        return tagDao.getTagByName(tagName)
    }

    override suspend fun checkpoint(): File = withContext(Dispatchers.IO) {
        dbManager.mutex.withLock {
            val database = dbManager.database.openHelper.writableDatabase

            // Execute the checkpoint
            database.query("PRAGMA checkpoint(FULL)").use { cursor ->
                if (cursor.moveToFirst()) {
                    val busy = cursor.getInt(0)
                    val log = cursor.getInt(1)
                    val checkpointed = cursor.getInt(2)
                    Log.d("kptest", "Checkpoint: Busy=$busy, Log=$log, Checkpointed=$checkpointed")
                } else {
                    // If it's empty, the command was still executed, just no status returned
                    Log.d("kptest", "Checkpoint executed (no status returned)")
                }
            }
            dbManager.getDatabaseFile()
        }
    }

    override suspend fun swapDatabase(dbFile: File) {
        withContext(NonCancellable) {
            synchronized(DatabaseManager::class.java) {
                dbManager.swapDatabase(dbFile)
                Log.d("kptest", "Swapping db successful (NonCancellable)")
            }
        }
    }

    override suspend fun triggerSync() {
        val settings = userPreferencesRepository.settingsFlow.first()
        if (settings.supaSyncEnabled) {
            enqueueSync()
        }
    }

    override suspend fun fetchCloudData() {
        val settings = userPreferencesRepository.settingsFlow.first()
        if (settings.supaSyncEnabled) {
            enqueueFetch()
        }
    }

    override fun startRealtimeSync() {
//        repositoryScope.launch {
//            val user = authRepository.getSignedInFirebaseUser().first()
//            val userId = user?.uid ?: return@launch
//            val settings = userPreferencesRepository.settingsFlow.first()
//            if(!settings.supaSyncEnabled) return@launch
//
//            val client = supabaseSyncService.getSupabaseClient()
//            val channel = client.channel("db-changes")
//
//            val noteFlow = channel.postgresChangeFlow<PostgrestAction.Update>(schema = "public") {
//                table = "note_entity"
//            }
//            val noteInsertFlow = channel.postgresChangeFlow<PostgrestAction.Insert>(schema = "public") {
//                table = "note_entity"
//            }
//
//            launch {
//                noteFlow.collect { action -> handleNoteChange(action.record, userId) }
//            }
//            launch {
//                noteInsertFlow.collect { action -> handleNoteChange(action.record, userId) }
//            }
//
//            // Similarly for tags and refs... (simplified for brevity but including base logic)
//            // In a real app, I'd subscribe to all tables.
//
//            channel.subscribe()
//        }
    }

//    private suspend fun handleNoteChange(dto: NoteDto, currentUserId: String) {
//        if (dto.userId != currentUserId) return
//        val local = noteDao.getNoteById(dto.id)
//        if (local == null || dto.lastUpdateTime > local.noteEntity.lastUpdateTime) {
//            noteDao.inserNote(dto.toEntity())
//            noteDao.updateNote(dto.toEntity())
//        }
//    }
}
