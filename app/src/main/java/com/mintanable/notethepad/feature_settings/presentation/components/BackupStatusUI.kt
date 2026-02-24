package com.mintanable.notethepad.feature_settings.presentation.components

import android.text.format.Formatter.formatFileSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import  androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_backup.presentation.BackupUiState
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import com.mintanable.notethepad.feature_backup.presentation.UploadDownload
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupStatusUI(
    backupUploadDownloadState: BackupStatus,
    backupUiState: BackupUiState,
    onRestoreClicked: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        val isRunning = backupUploadDownloadState is BackupStatus.Progress
        val progress = if (backupUploadDownloadState is BackupStatus.Progress) {
            backupUploadDownloadState.percentage * 1f
        } else {
            0f
        }

        if (isRunning && progress >= 0) {
            val type = if (backupUploadDownloadState is BackupStatus.Progress) { backupUploadDownloadState.type} else null
            Text(
                if(type==UploadDownload.UPLOAD) {
                    "Uploading to Drive: $progress%"
                } else {
                    "Downloading from Drive: $progress%"
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp)
            )
        }

        when (backupUiState) {
            is BackupUiState.Loading -> {
                Text("Checking cloud status...",
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
                        "Last cloud backup:\n$formattedDate (${
                            formatFileSize(
                                LocalContext.current,
                                backupUiState.metadata.size
                            )
                        })",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .weight(1f)
                    )

                    Button(
                        modifier = Modifier.padding(8.dp).clip(RectangleShape),
                        onClick = onRestoreClicked
                    ) {
                        Text("Restore Now")
                    }
                }
            }
            is BackupUiState.NoBackup -> {
                Text("No backups found in Drive",
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
            backupUploadDownloadState = BackupStatus.Idle,
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
            backupUploadDownloadState = BackupStatus.Idle,
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
            backupUploadDownloadState = BackupStatus.Idle,
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
            backupUploadDownloadState = BackupStatus.Progress(12, UploadDownload.UPLOAD),
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
            backupUploadDownloadState =  BackupStatus.Idle,
            backupUiState = BackupUiState.Error("No internet connection"),
            onRestoreClicked = {}
        )
    }
}