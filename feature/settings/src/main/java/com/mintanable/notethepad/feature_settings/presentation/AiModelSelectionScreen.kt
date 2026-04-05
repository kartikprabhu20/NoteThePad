package com.mintanable.notethepad.feature_settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.common.humanReadableSize
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import com.mintanable.notethepad.core.model.backup.DriveFileMetadata
import com.mintanable.notethepad.core.model.backup.LoadStatus
import com.mintanable.notethepad.core.model.backup.LoadType
import com.mintanable.notethepad.core.model.settings.BackupFrequency
import com.mintanable.notethepad.core.model.settings.BackupSettings
import com.mintanable.notethepad.core.model.settings.Settings
import com.mintanable.notethepad.feature_settings.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModelSelectionScreen(
    state: SettingsState,
    onBackPressed: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    showToast: (String) -> Unit
) {
    var modelToDelete by remember { mutableStateOf<AiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setting_ai_model_selection_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(state.aiModels) { model ->
                val isNanoUnsupported = model.name == "Gemini Nano (System)" &&
                        state.nanoStatus == AiModelDownloadStatus.Unavailable
                AiModelItem(
                    model = model,
                    state = state,
                    isSelected = model.name == state.settings.aiModelName,
                    isDisabled = isNanoUnsupported,
                    onSelect = {
                        if (!isNanoUnsupported) onEvent(SettingsEvent.SelectAiModel(model))
                    },
                    onDelete = { modelToDelete = model }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_ai_model_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_delete_ai_model_description,
                        model.displayName,
                        model.sizeInBytes.humanReadableSize()
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(SettingsEvent.DeleteAiModel(model))
                        modelToDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.btn_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    state.showDownloadModelDialog?.let { model ->
        AlertDialog(
            onDismissRequest = { onEvent(SettingsEvent.DismissDownloadDialog) },
            title = { Text(stringResource(R.string.dialog_download_ai_model_title)) },
            text = { Text(stringResource(R.string.dialog_download_ai_model_description, model.sizeInBytes.humanReadableSize())) },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(SettingsEvent.ConfirmDownloadAiModel(
                        aiModel = model,
                        onFailure = showToast
                    ))
                }) {
                    Text(stringResource(R.string.btn_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DismissDownloadDialog) }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun AiModelItem(
    model: AiModel,
    state: SettingsState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    isDisabled: Boolean = false
) {
    val contentAlpha = if (isDisabled) 0.5f else 1f
    val primaryTextColor = if (isDisabled)
        MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
    else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDisabled)
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
    else MaterialTheme.colorScheme.onSurfaceVariant

    val displayLabel = if (isDisabled) {
        model.displayName + " " + stringResource(R.string.ai_model_device_not_supported)
    } else {
        model.displayName
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isSelected,
                    enabled = !isDisabled,
                    onClick = onSelect
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                enabled = !isDisabled,
                onClick = onSelect
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                Text(
                    text = model.info,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (model.sizeInBytes > 0) {
                        Text(
                            text = stringResource(
                                R.string.dialog_download_ai_model_size,
                                model.sizeInBytes.humanReadableSize()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (model.isDownloaded) {
                        Text(
                            text = " " + stringResource(R.string.dialog_ai_model_downloaded),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (model.isDownloaded) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.content_description_delete_model),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (state.aiModelDownloadStatus is LoadStatus.Progress && state.aiModelDownloadStatus.fileName == model.downloadFileName) {
            Text(stringResource(R.string.msg_downloading_model, state.aiModelDownloadStatus.percentage),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LinearProgressIndicator(
                progress = { state.aiModelDownloadStatus.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
fun PreviewAiModelItem() {
    NoteThePadTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            AiModelItem(
                model = AiModel(
                    name = "Gemma3-1B-IT",
                    displayName = "Gemma 3 1B",
                    info = "Lightweight on-device model. Good balance between speed and performance.",
                    sizeInBytes = 584417280L,
                    minDeviceMemoryInGb = 6,
                    downloadFileName = "abc",
                    isDownloaded = true
                ),
                state = SettingsState(
                    aiModelDownloadStatus = LoadStatus.Progress(
                        percentage = 50,
                        type = LoadType.DOWNLOAD,
                        fileName = "abc"
                    )
                ),
                isSelected = true,
                onSelect = {},
                onDelete = {}
            )
        }
    }
}
