package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.ui.theme.RedOrange

@Composable
fun NoteItem(
    note: Note,
    modifier:Modifier=Modifier,
    cornerRadius: Dp = 10.dp,
    cutCornerSize : Dp = 30.dp,
    onDeleteClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
) {
    Box(
        modifier=modifier
    ){
       Canvas(modifier = Modifier.matchParentSize()){
           val clipPath = Path().apply{
               lineTo(size.width-cutCornerSize.toPx(),0f)
               lineTo(size.width, cutCornerSize.toPx())
               lineTo(size.width, size.height)
               lineTo(0f,size.height)
               close()
           }
           clipPath(clipPath){
                drawRoundRect(
                    color = Color(note.color),
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )

               drawRoundRect(
                   color = Color(ColorUtils.blendARGB(note.color,0x000000, 0.2f)
                   ),
                   topLeft = Offset(size.width-cutCornerSize.toPx(), -100f),
                   size = Size(cutCornerSize.toPx()+100f,cutCornerSize.toPx()+100f),
                   cornerRadius = CornerRadius(cornerRadius.toPx())
               )
           }
       }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(end = 32.dp)
        ) {

            with(sharedTransitionScope) {

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "note-title-${note.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "note-content-${note.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }
        }
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.align(alignment = Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete note",
                tint = MaterialTheme.colors.onSurface
            )

        }
    }
}


@Preview(showBackground = true)
@Composable
fun NoteItemPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
        NoteItem(
            note = Note(
                title = "Meeting Notes",
                content = "Discuss the new architecture for the JioHotstar platform. Focus on performance and scalability.",
                timestamp = System.currentTimeMillis(),
                color = RedOrange.toArgb(),
                id = 1,
                imageUris = emptyList()
            ),
            modifier = Modifier.fillMaxWidth(),
            onDeleteClick = {},
            sharedTransitionScope = this@SharedTransitionLayout,
            animatedVisibilityScope = this@AnimatedVisibility as AnimatedContentScope
        )
            }
        }
    }
}