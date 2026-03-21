package com.mintanable.notethepad.feature_backup.domain.repository

import com.mintanable.notethepad.core.model.backup.DriveFileMetadata
import com.mintanable.notethepad.core.model.backup.LoadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

interface GoogleDriveRepository{
    suspend fun uploadFileWithProgress(accessToken: String, file: File, driveFileName: String): Flow<LoadStatus>
    suspend fun downloadFile(accessToken: String, targetFile: File, driveFileName: String): Flow<LoadStatus>
    suspend fun checkForExistingBackup(accessToken: String): Flow<DriveFileMetadata?>
    suspend fun uploadMultipleFiles(accessToken: String, files: List<Pair<File, String>>): Flow<LoadStatus>
}