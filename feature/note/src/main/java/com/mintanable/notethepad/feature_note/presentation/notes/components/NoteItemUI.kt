package com.mintanable.notethepad.feature_note.presentation.notes.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.components.drawNoteShape
import com.mintanable.notethepad.components.drawNoteWithImage
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.core.richtext.compose.RichTextAnnotator
import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.RedOrange
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun NoteItemUI(
    note: DetailedNote,
    modifier:Modifier=Modifier,
    noteShape: NoteShape = NoteShape.DEFAULT,
    enableDeleteIcon: Boolean = true,
    onDeleteClick: () -> Unit,
    onPinClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    isDarkTheme: Boolean = false
) {
    Box(
        modifier=modifier.height(IntrinsicSize.Min)
    ){
        val resolvedColor = NoteColors.resolveDisplayColor(note.color, isDarkTheme).toArgb()
        val resolvedBackgroundRes = NoteColors.resolveBackgroundImage(note.backgroundImage, isDarkTheme)
        val hasBackgroundImage = note.backgroundImage != -1
        val imagePainter = if (hasBackgroundImage) {
            painterResource(id = resolvedBackgroundRes)
        } else {
            null
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            if (hasBackgroundImage) {
                drawNoteWithImage(noteShape, noteColorInt = resolvedColor, imagePainter = imagePainter, isDarkTheme=isDarkTheme)
            } else {
                drawNoteShape(noteShape, resolvedColor)
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
        ) {

            with(sharedTransitionScope) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(end = 32.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "note-title-${note.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))

                if(!note.isCheckboxListAvailable){
                    val contentAnnotated = remember(note.content) {
                        RichTextAnnotator.toAnnotatedString(RichTextSerializer.deserialize(note.content))
                    }
                    Text(
                        text = contentAnnotated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "note-content-${note.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
                } else {
                    SimpleCheckboxList( checklist = note.checkListItems )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    if(note.imageUris.isNotEmpty()){
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = stringResource(R.string.content_description_images_attached),
                            modifier = Modifier.alpha(alpha = 0.5f),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if(note.audioAttachments.isNotEmpty()){
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.content_description_audio_attached),
                            modifier = Modifier.alpha(alpha = 0.5f),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if(note.reminderTime > -1){
                        Icon(
                            imageVector = if(note.reminderTime> System.currentTimeMillis())
                                Icons.Default.Notifications
                            else
                                Icons.Default.NotificationsOff,
                            contentDescription = stringResource(R.string.content_description_reminder_set),
                            modifier = Modifier.alpha(alpha = 0.5f),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    if(note.checkListItems.isNotEmpty()){
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = stringResource(R.string.content_description_checkboxes_available),
                            modifier = Modifier.alpha(alpha = 0.5f),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    if(note.tagEntities.isNotEmpty()){
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = stringResource(R.string.content_description_tags_available),
                            modifier = Modifier.alpha(alpha = 0.5f),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if(enableDeleteIcon) {
                        IconButton(
                            onClick = onPinClick,
                            modifier = Modifier.size(24.dp).padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(R.string.content_description_pin_note),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.content_description_delete_note),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


@ThemePreviews
@Composable
fun NoteItemUIPreviewCheckboxes() {
    val isDark = isSystemInDarkTheme()
    NoteThePadTheme(darkTheme = isDark) {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview", modifier = Modifier.background(MaterialTheme.colorScheme.surface)) { isVisible ->
                if (isVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        NoteItemUI(
                            note = DetailedNote(
                                title = "Meeting Notes",
                                content = "Discuss the new architecture for the platform.",
                                timestamp = System.currentTimeMillis(),
                                color = RedOrange.toArgb(),
                                id = "1",
                                imageUris = listOf("image"),
                                audioAttachments = listOf(Attachment("x",123)),
                                reminderTime = 1,
                                checkListItems = listOf(CheckboxItem(text = "abc")),
                                isCheckboxListAvailable = true,
                                tagEntities = listOf(TagEntity("abc"))
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            onDeleteClick = {},
                            onPinClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            isDarkTheme = isDark
                        )
                    }
                }
            }
        }
    }
}


@ThemePreviews
@Composable
fun NoteItemUIPreview() {
    val isDark = isSystemInDarkTheme()
    NoteThePadTheme(darkTheme = isDark) {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview", modifier = Modifier.background(MaterialTheme.colorScheme.surface)) { isVisible ->
                if (isVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        NoteItemUI(
                            note = DetailedNote(
                                title = "Meeting Notes",
                                content = "Discuss the new architecture for the platform.",
                                timestamp = System.currentTimeMillis(),
                                color = RedOrange.toArgb(),
                                id = "1",
                                imageUris = listOf("image"),
                                audioAttachments = listOf(Attachment("x",123)),
                                reminderTime = 1,
                                tagEntities = listOf(TagEntity("abc"))
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            onDeleteClick = {},
                            onPinClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            isDarkTheme = isDark
                        )
                    }
                }
            }
        }
    }
}


@ThemePreviews
@Composable
fun NoteItemUIBackgrroundImagePreview() {
    val isDark = isSystemInDarkTheme()
    NoteThePadTheme(darkTheme = isDark) {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview", modifier = Modifier.background(MaterialTheme.colorScheme.surface)) { isVisible ->
                if (isVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        NoteItemUI(
                            note = DetailedNote(
                                title = "Meeting Notes",
                                content = "Discuss the new architecture for the platform.",
                                timestamp = System.currentTimeMillis(),
                                color = -1,
                                id = "1",
                                imageUris = listOf("image"),
                                audioAttachments = listOf(Attachment("x",123)),
                                reminderTime = 1,
                                tagEntities = listOf(TagEntity("abc")),
                                backgroundImage = 2
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            onDeleteClick = {},
                            onPinClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            isDarkTheme = isDark
                        )
                    }
                }
            }
        }
    }
}