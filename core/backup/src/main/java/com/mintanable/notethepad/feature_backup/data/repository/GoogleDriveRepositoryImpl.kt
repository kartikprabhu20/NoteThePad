package com.mintanable.notethepad.feature_backup.data.repository

import android.util.Log
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.FileContent
import com.mintanable.notethepad.core.model.NoteThePadConstants.BACKUP_DB_FILE_NAME
import com.mintanable.notethepad.core.model.backup.DriveFileMetadata
import com.mintanable.notethepad.core.model.backup.LoadStatus
import com.mintanable.notethepad.core.model.backup.LoadType
import com.mintanable.notethepad.feature_backup.domain.GoogleDriveService
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleDriveRepository
import com.mintanable.notethepad.file.AttachmentHelper.getMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class GoogleDriveRepositoryImpl @Inject constructor(
    private val driveService: GoogleDriveService
): GoogleDriveRepository {

    override suspend fun uploadFileWithProgress(accessToken: String, file: File, driveFileName: String): Flow<LoadStatus> = callbackFlow {
        try {
            Log.d("kptest", "uploadFileWithProgressr: ${driveFileName}")

            val driveService = driveService.getDriveService(accessToken)

            val existingFileId = withContext(Dispatchers.IO) {
                driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$driveFileName'")
                    .setFields("files(id)")
                    .execute()
                    .files?.firstOrNull()?.id
            }

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = driveFileName
                if (existingFileId == null) {
                    parents = listOf("appDataFolder")
                }
            }

            val mediaContent = FileContent(getMimeType(file.absolutePath), file)

            val uploadRequest = if (existingFileId != null) {
                driveService.files().update(existingFileId, fileMetadata, mediaContent)
            } else {
                driveService.files().create(fileMetadata, mediaContent)
            }

            val uploader = uploadRequest.mediaHttpUploader
            uploader.isDirectUploadEnabled = false
            uploader.chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE

            uploader.setProgressListener {
                Log.d("kptest", "uploader listener: ${uploader.uploadState}")

                when (uploader.uploadState) {
                    MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                        val percent = (uploader.progress * 100).toInt()
                        Log.d("kptest", "upload progress: $percent")
                        trySend(LoadStatus.Progress(percent, LoadType.UPLOAD))
                    }
                    MediaHttpUploader.UploadState.MEDIA_COMPLETE -> {
                        Log.d("kptest", "upload successful")
                        trySend(LoadStatus.Progress(100, LoadType.UPLOAD))
                        trySend(LoadStatus.Success)
                        close()
                    }
                    else -> {}
                }
            }
            withContext(Dispatchers.IO) {
                uploadRequest.execute()
            }
            awaitClose {
                Log.d("kptest", "Flow channel closed.")
            }
        } catch (e: Exception) {
            trySend(LoadStatus.Error(e.message ?: "Upload failed"))
            close(e)
        }
    }

    override suspend fun downloadFile(accessToken: String, targetFile: File, driveFileName: String) : Flow<LoadStatus> = callbackFlow {
        try {
            Log.d("kptest", "downloadFile: ${driveFileName}")

            val driveService = driveService.getDriveService(accessToken)

            val fileId = withContext(Dispatchers.IO) {
                driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$driveFileName'")
                    .setFields("files(id)")
                    .execute()
                    .files?.firstOrNull()?.id
            } ?: throw Exception("File $driveFileName not found on Google Drive")

            val request = driveService.files().get(fileId)

            request.mediaHttpDownloader.apply {
                isDirectDownloadEnabled = false
                chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE
                setProgressListener { downloader ->
                    when (downloader.downloadState) {
                        MediaHttpDownloader.DownloadState.MEDIA_IN_PROGRESS -> {
                            val percent = (downloader.progress * 100).toInt()
                            trySend(LoadStatus.Progress(percent, LoadType.DOWNLOAD))
                        }
                        MediaHttpDownloader.DownloadState.MEDIA_COMPLETE -> {
                            trySend(LoadStatus.Progress(100, LoadType.DOWNLOAD))
                            trySend(LoadStatus.Success)
                            close()
                        }
                        else -> Unit
                    }
                }
            }

            withContext(Dispatchers.IO) {
                FileOutputStream(targetFile).use { outputStream ->
                    request.executeMediaAndDownloadTo(outputStream)
                }
            }

            close()
            awaitClose { }
        } catch (e: Exception) {
            trySend(LoadStatus.Error(e.message ?: "Download failed"))
            close(e)
        }
    }

    override suspend fun checkForExistingBackup(accessToken: String) : Flow<DriveFileMetadata?> = flow {
        try {
            val service = driveService.getDriveService(accessToken)

            val result = withContext(Dispatchers.IO) {
                service.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$BACKUP_DB_FILE_NAME'")
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
                        size = googleFile.getSize()
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

    override suspend fun uploadMultipleFiles(accessToken: String, files: List<Pair<File, String>>): Flow<LoadStatus> = callbackFlow {
        val totalSize = files.sumOf { it.first.length() }
        val progressMap = mutableMapOf<String, Long>()
        val drive = driveService.getDriveService(accessToken)

        files.forEach { (file, driveFileName) ->
            Log.d("kptest", "Uploading $driveFileName to Google Drive")

            try {
                val existingFileId = withContext(Dispatchers.IO) {
                    drive.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '$driveFileName'")
                        .setFields("files(id)")
                        .execute().files?.firstOrNull()?.id
                }

                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = driveFileName
                    if (existingFileId == null) parents = listOf("appDataFolder")
                }

                val mediaContent = FileContent(getMimeType(file.absolutePath), file)
                val uploadRequest = if (existingFileId != null) {
                    drive.files().update(existingFileId, fileMetadata, mediaContent)
                } else {
                    drive.files().create(fileMetadata, mediaContent)
                }

                uploadRequest.mediaHttpUploader.apply {
                    isDirectUploadEnabled = false
                    chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE
                    setProgressListener { uploader ->
                        if (uploader.uploadState == MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS) {
                            val currentFileBytes = (uploader.progress * file.length()).toLong()
                            progressMap[driveFileName] = currentFileBytes

                            // Calculate TOTAL progress
                            val totalUploaded = progressMap.values.sum()
                            val totalPercent = ((totalUploaded.toDouble() / totalSize) * 100).toInt()
                            Log.d("kptest", "upload progress: $totalPercent totalUploaded: $totalUploaded totalSize: $totalSize")
                            trySend(LoadStatus.Progress(totalPercent.coerceAtMost(99), LoadType.UPLOAD, totalUploaded, totalSize))
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    uploadRequest.execute()
                }
                // Mark this file as 100% complete in our map
                progressMap[driveFileName] = file.length()

            } catch (e: Exception) {
                Log.e("kptest", "Failed to upload $driveFileName", e)
            }
        }

        trySend(LoadStatus.Progress(100, LoadType.UPLOAD))
        trySend(LoadStatus.Success)
        close()
        awaitClose { }
    }

    override suspend fun downloadMultipleFiles(accessToken: String, files: List<Pair<File, String>>): Flow<LoadStatus> = callbackFlow {
        val drive = driveService.getDriveService(accessToken)

        // Get metadata for all files to calculate total expected bytes
        val totalSize = withContext(Dispatchers.IO) {
            files.sumOf { (_, driveName) ->
                val response = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$driveName' and trashed = false")
                    .setFields("files(id, size)")
                    .execute()

                // Use the first file found and get its size
                val driveFile = response.files?.firstOrNull()
                Log.d("kptest", "File: $driveName, Size: ${driveFile?.getSize()}")

                driveFile?.getSize() ?: 0L
            }
        }

        Log.d("kptest", "Media Download size: $totalSize")

        val progressMap = mutableMapOf<String, Long>()

        files.forEach { (targetFile, driveFileName) ->
            val fileId = withContext(Dispatchers.IO) {
                drive.files().list().setSpaces("appDataFolder")
                    .setQ("name = '$driveFileName'")
                    .setFields("files(id, size)").execute().files?.firstOrNull()
            } ?: return@forEach // Skip if missing

            val request = drive.files().get(fileId.id)
            val fileSize = fileId.getSize() ?: 0L

            request.mediaHttpDownloader.apply {
                isDirectDownloadEnabled = false
                setProgressListener { downloader ->
                    val currentBytes = (downloader.progress * fileSize).toLong()
                    progressMap[driveFileName] = currentBytes

                    val totalDownloaded = progressMap.values.sum()
                    val totalPercent = ((totalDownloaded.toDouble() / totalSize) * 100).toInt()
                    Log.d("kptest", "Downloading backup: $totalPercent totalDownloaded: $totalDownloaded totalSize: $totalSize")

                    trySend(LoadStatus.Progress(
                        percentage = totalPercent.coerceAtMost(99),
                        bytes = totalDownloaded,
                        totalBytes = totalSize,
                        type = LoadType.DOWNLOAD
                    ))
                }
            }

            withContext(Dispatchers.IO) {
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { request.executeMediaAndDownloadTo(it) }
            }
            progressMap[driveFileName] = fileSize
        }

        trySend(LoadStatus.Progress(100,LoadType.DOWNLOAD , totalSize, totalSize))
        trySend(LoadStatus.Success)
        close()
        awaitClose { }
    }

    override suspend fun getAllBackupFileNames(accessToken: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService.getDriveService(accessToken)
            val fileNames = mutableListOf<String>()
            var pageToken: String? = null

            do {
                val result = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("trashed = false")
                    .setFields("nextPageToken, files(name)")
                    .setPageToken(pageToken)
                    .setPageSize(100)
                    .execute()
                result.files?.forEach { file ->
                    file.name?.let { fileNames.add(it) }
                }

                pageToken = result.nextPageToken
            } while (pageToken != null)

            Log.d("kptest", "Found ${fileNames.size} files in appDataFolder")
            fileNames
        } catch (e: Exception) {
            Log.e("kptest", "Error fetching all filenames", e)
            emptyList()
        }
    }

    override suspend fun clearAppDataFolder(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService.getDriveService(accessToken)
            var pageToken: String? = null
            var deletedCount = 0

            Log.d("kptest", "Starting full cleanup of appDataFolder...")

            do {
                val result = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute()

                result.files?.forEach { file ->
                    drive.files().delete(file.id).execute()
                    deletedCount++
                    Log.d("kptest", "Deleted from Drive: ${file.name}")
                }

                pageToken = result.nextPageToken
            } while (pageToken != null)

            Log.d("kptest", "Cleanup complete. Total files removed: $deletedCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("kptest", "Failed to clear appDataFolder", e)
            Result.failure(e)
        }
    }
}
