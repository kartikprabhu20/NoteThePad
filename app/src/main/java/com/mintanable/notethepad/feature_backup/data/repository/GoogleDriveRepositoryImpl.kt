package com.mintanable.notethepad.feature_backup.data.repository

import android.util.Log
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.FileContent
import com.mintanable.notethepad.feature_backup.domain.GoogleDriveService
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class GoogleDriveRepositoryImpl @Inject constructor(
    private val driveService: GoogleDriveService
): GoogleDriveRepository {

    companion object{
        const val BACKUP_FILE_NAME = "NoteThePad_Backup.db"
    }
    override suspend fun uploadFileWithProgress(accessToken: String, dbFile: File): Flow<BackupStatus> = callbackFlow {
        try {
            val driveService = driveService.getDriveService(accessToken)

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }
            val mediaContent = FileContent("application/x-sqlite3", dbFile)

            val uploadRequest = driveService.files().create(fileMetadata, mediaContent)
            val uploader = uploadRequest.mediaHttpUploader
            uploader.isDirectUploadEnabled = false
            uploader.chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE

            uploader.setProgressListener {
                Log.d("kptest", "uploader listener: ${uploader.uploadState}")

                when (uploader.uploadState) {
                    MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                        val percent = (uploader.progress * 100).toInt()
                        Log.d("kptest", "upload progress: $percent")
                        trySend(BackupStatus.Progress(percent))
                    }
                    MediaHttpUploader.UploadState.MEDIA_COMPLETE -> {
                        Log.d("kptest", "upload successful")
                        trySend(BackupStatus.Success)
                    }
                    else -> {}
                }
            }
            withContext(Dispatchers.IO) {
                uploadRequest.execute()
            }
            awaitClose {
                Log.d("kptest", "upload complete")
            }
        } catch (e: Exception) {
            trySend(BackupStatus.Error(e.message ?: "Upload failed"))
            close(e)
        }
    }


    override suspend fun downloadBackupFile() {
        TODO("Not yet implemented")
    }

    override suspend fun checkForExistingBackup(accessToken: String) : Flow<DriveFileMetadata?> = flow {
        try {
            val service = driveService.getDriveService(accessToken)

            val result = withContext(Dispatchers.IO) {
                service.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$BACKUP_FILE_NAME'")
                    .setFields("files(id, name, modifiedTime, size)")
                    .execute()
            }

            val googleFile = result.files.firstOrNull()

            if (googleFile != null) {
                emit(
                    DriveFileMetadata(
                        id = googleFile.id,
                        name = googleFile.name,
                        modifiedTime = googleFile.modifiedTime.value,
                        size = googleFile.size.toLong()
                    )
                )
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            Log.d("kptest", "Exception while checking for existing backup in google drive $e")
            emit(null)
        }
    }
}