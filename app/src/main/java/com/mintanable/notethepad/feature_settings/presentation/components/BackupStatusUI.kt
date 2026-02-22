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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.mintanable.notethepad.feature_backup.presentation.BackupUiState
import com.mintanable.notethepad.feature_backup.presentation.DriveFileMetadata
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun BackupStatusUI(
    workInfo: WorkInfo?,
    backupUiState: BackupUiState,
    onRestoreClicked: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        val progress = workInfo?.progress?.getInt("percent", -1) ?: -1
        val isRunning = workInfo?.state == WorkInfo.State.RUNNING

        if (isRunning && progress >= 0) {
            Text("Uploading to Drive: $progress%", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBackupHasData() {
    NoteThePadTheme {
        BackupStatusUI(
            workInfo = null,
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
            workInfo = null,
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
            workInfo = null,
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
            workInfo = WorkInfo(
                id = UUID.fromString("test"),
                state = WorkInfo.State.RUNNING,
                tags = setOf("testTag")
            ),
            backupUiState = BackupUiState.HasBackup(
                DriveFileMetadata("1", "Notes.db", 1708600000000L, 1024 * 1024 * 2) // 2MB
            ),
            onRestoreClicked = {}
        )
    }
}