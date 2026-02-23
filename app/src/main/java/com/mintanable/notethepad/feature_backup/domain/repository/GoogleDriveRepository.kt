package com.mintanable.notethepad.feature_backup.domain.repository

import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import kotlinx.coroutines.flow.Flow
import java.io.File

interface GoogleDriveRepository{
    suspend fun uploadFileWithProgress(accessToken: String, dbFile: File): Flow<BackupStatus>
    suspend fun downloadBackupFile(accessToken: String, dbFile: File): Flow<BackupStatus>
    suspend fun checkForExistingBackup(accessToken: String): Flow<DriveFileMetadata?>
}