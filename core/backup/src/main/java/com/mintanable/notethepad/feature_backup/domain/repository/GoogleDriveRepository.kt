package com.mintanable.notethepad.feature_backup.domain.repository

import com.mintanable.notethepad.core.model.backup.DriveFileMetadata
import com.mintanable.notethepad.core.model.backup.LoadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

interface GoogleDriveRepository{
    suspend fun uploadFileWithProgress(accessToken: String, dbFile: File): Flow<LoadStatus>
    suspend fun downloadBackupFile(accessToken: String, dbFile: File): Flow<LoadStatus>
    suspend fun checkForExistingBackup(accessToken: String): Flow<DriveFileMetadata?>
}