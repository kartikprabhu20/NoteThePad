package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.core.model.note.MediaState
import com.mintanable.notethepad.feature_note.presentation.modify.components.AudioPlayerUI

fun LazyListScope.audioAttachmentSection(
    attachedAudios: List<Attachment>,
    mediaState: MediaState?,
    onDelete: (String) -> Unit,
    onPlayPause: (String) -> Unit,
    onTranscribe: (String) -> Unit
) {

    if (attachedAudios.isNotEmpty()) {
        item{
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                attachedAudios.forEach { audioUri ->
                    AudioPlayerUI(
                        attachment = audioUri,
                        playbackState = mediaState,
                        onDelete = onDelete,
                        onPlayPause = onPlayPause,
                        onTranscribe = onTranscribe
                    )
                }
            }
        }
    }
}