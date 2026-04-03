package com.mintanable.notethepad.feature_note.presentation.archive

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mintanable.notethepad.feature_note.R

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
    noteWithTags: com.mintanable.notethepad.database.db.entity.NoteWithTags,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    Card(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = noteWithTags.noteEntity.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = noteWithTags.noteEntity.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                Column {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                    }
                    IconButton(onClick = onDeletePermanently) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}
