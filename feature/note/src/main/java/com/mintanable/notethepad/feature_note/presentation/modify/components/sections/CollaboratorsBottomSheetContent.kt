package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mintanable.notethepad.core.model.collaboration.Collaborator
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun CollaboratorsBottomSheetContent(
    collaborators: List<Collaborator>,
    isLoading: Boolean,
    errorMessage: String?,
    isOwner: Boolean,
    onInvite: (String) -> Unit,
    onRemove: (String) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var emailInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.option_collaborate),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isOwner) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = { Text(stringResource(R.string.invite_collaborator)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            onInvite(emailInput.trim())
                            emailInput = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.invite)
                    )
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        LazyColumn {
            items(collaborators, key = { it.userId }) { collaborator ->
                CollaboratorRow(
                    collaborator = collaborator,
                    isOwner = isOwner,
                    onRemove = { onRemove(collaborator.userId) },
                    onLeave = onLeave
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CollaboratorRow(
    collaborator: Collaborator,
    isOwner: Boolean,
    onRemove: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            CollaboratorAvatar(
                displayName = collaborator.displayName,
                photoUrl = collaborator.photoUrl,
                size = 40,
                isOwner = collaborator.isOwner
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = collaborator.displayName ?: collaborator.email,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (collaborator.displayName != null) {
                    Text(
                        text = collaborator.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (collaborator.isOwner) {
                    Text(
                        text = stringResource(R.string.owner_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (!collaborator.isOwner) {
            if (isOwner) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_collaborator),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else if(collaborator.isCurrentUser) {
                TextButton(onClick = onLeave) {
                    Text(
                        stringResource(R.string.leave_note),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CollaboratorAvatar(
    displayName: String?,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Int = 32,
    isOwner: Boolean = false
) {
    val ownerBorder = if (isOwner) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }
    if (!photoUrl.isNullOrBlank() && photoUrl != "null") {
        AsyncImage(
            model = photoUrl,
            contentDescription = displayName,
            modifier = modifier
                .size(size.dp)
                .then(ownerBorder)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initial = (displayName?.firstOrNull() ?: '?').uppercaseChar()
        Box(
            modifier = modifier
                .size(size.dp)
                .then(ownerBorder)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@ThemePreviews
@Composable
fun CollaboratorsBottomSheetContentPreview() {
    val collaborators = listOf(
        Collaborator(
            id = "1",
            noteId = "note1",
            userId = "user1",
            email = "owner@example.com",
            displayName = "Owner Name",
            photoUrl = null,
            isOwner = true
        ),
        Collaborator(
            id = "2",
            noteId = "note1",
            userId = "user2",
            email = "collaborator@example.com",
            displayName = "Collaborator Name",
            photoUrl = null,
            isOwner = false
        )
    )
    NoteThePadTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            CollaboratorsBottomSheetContent(
                collaborators = collaborators,
                isLoading = false,
                errorMessage = null,
                isOwner = true,
                onInvite = {},
                onRemove = {},
                onLeave = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun CollaboratorsBottomSheetContentLoadingPreview() {
    NoteThePadTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            CollaboratorsBottomSheetContent(
                collaborators = emptyList(),
                isLoading = true,
                errorMessage = null,
                isOwner = true,
                onInvite = {},
                onRemove = {},
                onLeave = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun CollaboratorsBottomSheetContentErrorPreview() {
    NoteThePadTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            CollaboratorsBottomSheetContent(
                collaborators = emptyList(),
                isLoading = false,
                errorMessage = "Failed to load collaborators",
                isOwner = true,
                onInvite = {},
                onRemove = {},
                onLeave = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollaboratorAvatarPreview() {
    NoteThePadTheme {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CollaboratorAvatar(displayName = "John Doe", photoUrl = null)
            CollaboratorAvatar(displayName = null, photoUrl = null)
        }
    }
}
