package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mintanable.notethepad.R
import com.mintanable.notethepad.core.model.AiModel
import com.mintanable.notethepad.feature_ai.presentation.humanReadableSize
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun AiModelSelectionDialog(
    currentModel: String,
    aiModels: List<AiModel>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedModel by remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_select_ai_model_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                aiModels.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (model.name == selectedModel),
                                onClick = { selectedModel = model.name }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (model.name == selectedModel),
                            onClick = { selectedModel = model.name}
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = model.info,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.dialog_download_ai_model_size, model.sizeInBytes.humanReadableSize()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedModel) }) {
                Text(stringResource(R.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@ThemePreviews
@Composable
fun PreviewAiModelSelectionDialog(modifier: Modifier = Modifier) {

    val testModels = listOf(
        AiModel(
            name = "None",
            displayName = "None",
            info = "No AI assistance will be provided.",
        ),
        AiModel(
            name = "Gemini 3 Flash (Cloud)",
            displayName = "Gemini 3 Flash (Cloud)",
            info = "Fastest response, requires internet connection.",
        ),
        AiModel(
            name = "Gemini Nano (System)",
            displayName = "Gemini Nano (System)",
            info = "On-device privacy and performance. Only on supported devices.",
        ),

        AiModel(
            name = "Gemma3-1B-IT",
            displayName = "Gemma 3 1B",
            info = "Lightweight on-device model. Good balance between speed and performance.",
            sizeInBytes = 584417280L,
            minDeviceMemoryInGb = 6,
        ),
        AiModel(
            name = "Gemma-3n-E2B-it",
            displayName = "Gemma 3 E2B",
            info = "Higher quality on-device reasoning. Requires more storage and RAM.",
            sizeInBytes = 3655827456L,
            minDeviceMemoryInGb = 8,
        ),
        AiModel(
            name = "Gemma-3n-E4B-it",
            displayName = "Gemma 3 E4B",
            info = "Best on-device performance. Recommended for powerful devices only.",
            sizeInBytes = 4919541760L,
            minDeviceMemoryInGb = 12,
        )
    )

    NoteThePadTheme {
        AiModelSelectionDialog(
            currentModel = "None",
            aiModels = testModels,
            onDismiss = {},
            onConfirm = {}
        )
    }
}