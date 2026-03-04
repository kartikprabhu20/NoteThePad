package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.util.Attachment
import com.mintanable.notethepad.feature_note.domain.util.MediaState
import com.mintanable.notethepad.feature_note.presentation.modify.components.AudioPlayerUI

fun LazyListScope.audioAttachmentSection(
    attachedAudios: List<Attachment>,
    mediaState: MediaState?,
    onDelete: (Uri) -> Unit,
    onPlayPause: (Uri) -> Unit
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
                        onPlayPause = onPlayPause
                    )
                }
            }
        }
    }
}