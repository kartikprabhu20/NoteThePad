package com.mintanable.notethepad.feature_settings.presentation.components

import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.common.humanReadableSize
import com.mintanable.notethepad.core.model.backup.DriveFileMetadata
import com.mintanable.notethepad.core.model.backup.LoadStatus
import com.mintanable.notethepad.core.model.backup.LoadType
import com.mintanable.notethepad.feature_settings.R
import com.mintanable.notethepad.feature_settings.presentation.BackupUiState
import com.mintanable.notethepad.theme.NoteThePadTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupStatusUI(
    backupUploadDownloadState: LoadStatus,
    backupUiState: BackupUiState,
    onRestoreClicked: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        val isRunning = backupUploadDownloadState is LoadStatus.Progress

        if (isRunning && backupUploadDownloadState.percentage >= 0) {
            Text(
                if(backupUploadDownloadState.type== LoadType.UPLOAD) {
                    if(backupUploadDownloadState.totalBytes > 0){
                        stringResource(R.string.msg_uploading_to_drive_with_bytes,
                            backupUploadDownloadState.percentage, backupUploadDownloadState.bytes.humanReadableSize(),
                            backupUploadDownloadState.totalBytes.humanReadableSize())
                    }else{
                        stringResource(R.string.msg_uploading_to_drive, backupUploadDownloadState.percentage)
                    }
                } else {
                    if(backupUploadDownloadState.totalBytes > 0){
                        stringResource(R.string.msg_downloading_from_drive_with_bytes,
                            backupUploadDownloadState.percentage, backupUploadDownloadState.bytes.humanReadableSize(),
                            backupUploadDownloadState.totalBytes.humanReadableSize())
                    }else {
                        stringResource(
                            R.string.msg_downloading_from_drive, backupUploadDownloadState.percentage
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LinearProgressIndicator(
                progress = { backupUploadDownloadState.percentage / 100f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp)
            )
        }

        when (backupUiState) {
            is BackupUiState.Loading -> {
                Text(stringResource(R.string.msg_checking_cloud_status),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            is BackupUiState.HasBackup -> {
                val formattedDate = remember(backupUiState.metadata.modifiedTime) {
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(Date(backupUiState.metadata.modifiedTime))
                }

                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.msg_last_cloud_backup, formattedDate, formatFileSize(context, backupUiState.metadata.size)),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .weight(1f)
                    )

                    Button(
                        modifier = Modifier.padding(8.dp).clip(RectangleShape),
                        onClick = onRestoreClicked
                    ) {
                        Text(stringResource(R.string.btn_restore_now))
                    }
                }
            }
            is BackupUiState.NoBackup -> {
                Text(stringResource(R.string.msg_no_backups_found),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            is BackupUiState.Error -> {
                Text( backupUiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBackupHasData() {
    NoteThePadTheme {
        BackupStatusUI(
            backupUploadDownloadState = LoadStatus.Idle,
            backupUiState = BackupUiState.HasBackup(
                DriveFileMetadata("1", "Notes.db", 1708600000000L, 1024 * 1024 * 2) // 2MB
            ),
            onRestoreClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBackupUINoBackup() {
    NoteThePadTheme {
        BackupStatusUI(
            backupUploadDownloadState = LoadStatus.Idle,
            backupUiState = BackupUiState.NoBackup,
            onRestoreClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBackupUILoading() {
    NoteThePadTheme {
        BackupStatusUI(
            backupUploadDownloadState = LoadStatus.Idle,
            backupUiState = BackupUiState.Loading,
            onRestoreClicked = {}
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewBackupUIWorkInfoUploading() {
    NoteThePadTheme {
        BackupStatusUI(
            backupUploadDownloadState = LoadStatus.Progress(12, LoadType.UPLOAD),
            backupUiState = BackupUiState.HasBackup(
                DriveFileMetadata("1", "Notes.db", 1708600000000L, 1024 * 1024 * 2) // 2MB
            ),
            onRestoreClicked = {}
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewBackupUIWorkInfoUploadingWithBytes() {
    NoteThePadTheme {
        BackupStatusUI(
            backupUploadDownloadState = LoadStatus.Progress(12, LoadType.UPLOAD, 20000, 4000000),
            backupUiState = BackupUiState.HasBackup(
                DriveFileMetadata("1", "Notes.db", 1708600000000L, 1024 * 1024 * 2) // 2MB
            ),
            onRestoreClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBackupUIError() {
    NoteThePadTheme {
        BackupStatusUI(
            backupUploadDownloadState =  LoadStatus.Idle,
            backupUiState = BackupUiState.Error("No internet connection"),
            onRestoreClicked = {}
        )
    }
}