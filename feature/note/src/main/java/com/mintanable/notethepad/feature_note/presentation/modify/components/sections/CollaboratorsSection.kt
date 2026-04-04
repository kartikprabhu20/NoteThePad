package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.collaboration.Collaborator

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.collaboratorsSection(
    collaborators: List<Collaborator>,
    onCollaboratorClick: () -> Unit
) {
    if (collaborators.isNotEmpty()) {
        item {
            FlowRow(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { onCollaboratorClick() },
                horizontalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                collaborators.forEach { collaborator ->
                    CollaboratorAvatar(
                        displayName = collaborator.displayName,
                        photoUrl = collaborator.photoUrl,
                        size = 28,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}
