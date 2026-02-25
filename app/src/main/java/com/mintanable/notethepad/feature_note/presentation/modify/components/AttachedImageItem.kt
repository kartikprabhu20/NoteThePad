package com.mintanable.notethepad.feature_note.presentation.modify.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.videoFrameMillis
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType
import com.mintanable.notethepad.feature_note.presentation.notes.util.AttachmentHelper
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@Composable
fun AttachedImageItem(
    uri: Uri,
    onDelete: (Uri) -> Unit,
    onClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val attachmentType = rememberSaveable(uri) { AttachmentHelper.getAttachmentType(context, uri) }
    Log.d("kptest", "AttachedImageItem $uri $attachmentType")
    Box(
        modifier = modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = if(attachmentType== AttachmentType.IMAGE) {
                uri
            } else {
                ImageRequest.Builder(context)
                    .data(uri)
                    .videoFrameMillis(1000L)
                    .crossfade(true)
                    .build()
            },
            contentDescription = "Attached image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.clickable {
                onClick(uri)
            }.fillMaxWidth(),
        )

        if (attachmentType == AttachmentType.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clickable { onDelete(uri) }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewAttachedImageItem(){
    NoteThePadTheme {
        AttachedImageItem(
            uri = Uri.EMPTY,
            onDelete = {},
            onClick = {}
        )
    }
}