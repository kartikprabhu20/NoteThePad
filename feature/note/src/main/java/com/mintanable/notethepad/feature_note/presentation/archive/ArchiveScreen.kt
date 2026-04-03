package com.mintanable.notethepad.feature_note.presentation.archive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.components.drawNoteShape
import com.mintanable.notethepad.components.drawNoteWithImage
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.core.richtext.compose.RichTextAnnotator
import com.mintanable.notethepad.core.richtext.serializer.RichTextSerializer
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBackPressed: () -> Unit,
    isDarkTheme: Boolean = false,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val deletedNotes by viewModel.deletedNotes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (deletedNotes.isEmpty()) {
            val resource = R.drawable.trash_male
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(resource),
                    contentDescription = stringResource(R.string.content_description_empty_list)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                items(deletedNotes) { noteWithTags ->
                    ArchiveNoteItem(
                        noteWithTags = noteWithTags,
                        onRestore = { viewModel.restoreNote(noteWithTags.noteEntity.id) },
                        onDeletePermanently = { viewModel.deleteNotePermanently(noteWithTags.noteEntity.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArchiveNoteItem(
    noteWithTags: NoteWithTags,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val note = noteWithTags.noteEntity
    val resolvedColor = NoteColors.resolveDisplayColor(note.color, isDarkTheme).toArgb()
    val resolvedBackgroundRes = NoteColors.resolveBackgroundImage(note.backgroundImage)
    val hasBackgroundImage = note.backgroundImage != -1
    val imagePainter = if (hasBackgroundImage) painterResource(id = resolvedBackgroundRes) else null

    val contentAnnotated = remember(note.content) {
        RichTextAnnotator.toAnnotatedString(RichTextSerializer.deserialize(note.content))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(IntrinsicSize.Min)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (hasBackgroundImage) {
                drawNoteWithImage(
                    NoteShape.DEFAULT,
                    noteColorInt = resolvedColor,
                    imagePainter = imagePainter,
                    isDarkTheme = isDarkTheme
                )
            } else {
                drawNoteShape(NoteShape.DEFAULT, resolvedColor)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Adjust padding to match your note style
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contentAnnotated,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDeletePermanently,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete Permanently",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun ArchiveNoteItemPreview() {
    val mockNote = NoteEntity(
        id = "1L",
        title = "Archived Meeting Notes",
        content = "This is the content of the archived note. It might contain rich text data.",
        timestamp = System.currentTimeMillis(),
        color = NoteColors.colors[1].toArgb(),
        backgroundImage = -1
    )
    val mockNoteWithTags = NoteWithTags(noteEntity = mockNote)

    NoteThePadTheme() {
        Box(modifier = Modifier.padding(16.dp)) {
            ArchiveNoteItem(
                noteWithTags = mockNoteWithTags,
                onRestore = {},
                onDeletePermanently = {}
            )
        }
    }
}
