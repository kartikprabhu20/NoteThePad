package com.mintanable.notethepad.feature_note.presentation.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.mintanable.notethepad.theme.NoteThePadTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantDialog(
    state: AiAssistantState,
    onPromptChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onToggleTranscription: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    if (!state.visible) return

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                // Message List
                val listState = rememberLazyListState()
                LaunchedEffect(state.messages.size, state.isStreaming) {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        ChatBubble(message)
                    }
                    if (state.isStreaming && state.messages.none { it.role == MessageRole.ASSISTANT && it.text.isEmpty() }) {
                        item {
                            AssistantTypingIndicator()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.prompt,
                        onValueChange = onPromptChange,
                        placeholder = { Text("How can I help?") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            IconButton(onClick = onToggleTranscription) {
                                Icon(
                                    imageVector = if (state.isTranscribing) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "Transcription",
                                    tint = if (state.isTranscribing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    if (state.isStreaming) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onSubmit,
                            enabled = state.prompt.text.isNotBlank(),
                            modifier = Modifier
                                .background(
                                    color = if (state.prompt.text.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (state.prompt.text.isNotBlank())
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val bubbleShape = if (isUser) 
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    else 
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .sizeIn(maxWidth = 280.dp)
                .clip(bubbleShape)
                .background(if (message.isError) MaterialTheme.colorScheme.errorContainer else bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = if (isUser) "You" else "Assistant",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun AssistantTypingIndicator() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.sizeIn(maxHeight = 16.dp, maxWidth = 16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = "Thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiAssistantDialogPreview() {
    NoteThePadTheme {
        AiAssistantDialog(
            state = AiAssistantState(
                visible = true,
                messages = listOf(
                    ChatMessage(role = MessageRole.USER, text = "Hello!"),
                    ChatMessage(role = MessageRole.ASSISTANT, text = "Hi there! How can I help you today?"),
                    ChatMessage(role = MessageRole.USER, text = "Can you summarize my last note?"),
                    ChatMessage(role = MessageRole.ASSISTANT, text = "Sure! Your last note was about the upcoming project deadline and the key tasks involved."),
                    ChatMessage(role = MessageRole.ASSISTANT, text = "An error occurred while connecting to the AI service.", isError = true)
                ),
                prompt = TextFieldValue("What else can you do?")
            ),
            onPromptChange = {},
            onSubmit = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatBubblePreview() {
    NoteThePadTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatBubble(ChatMessage(role = MessageRole.USER, text = "This is a user message."))
            ChatBubble(ChatMessage(role = MessageRole.ASSISTANT, text = "This is an assistant response."))
            ChatBubble(ChatMessage(role = MessageRole.ASSISTANT, text = "This is an error message.", isError = true))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AssistantTypingIndicatorPreview() {
    NoteThePadTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AssistantTypingIndicator()
        }
    }
}
