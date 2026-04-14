package com.mintanable.notethepad.feature_note.presentation.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AiAssistantDialog(
    state: AiAssistantState,
    onPromptChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Assistant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = onPromptChange,
                    label = { Text("Ask the assistant…") },
                    placeholder = { Text("e.g. summarize my latest 5 notes") },
                    enabled = !state.isStreaming,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.response.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = state.response,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (state.isStreaming) {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = state.prompt.isNotBlank() && !state.isStreaming,
            ) {
                Text(if (state.isStreaming) "Working…" else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
