package com.mintanable.notethepad.feature_note.presentation.notes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.modify.components.AttachedImageItem
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun ImageCollectionUI(
    modifier: Modifier = Modifier,
    imageUris: List<String> = emptyList()
) {
    if (imageUris.isEmpty()) return
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                shape = shape
            )
            .clip(shape)
    ) {
        if (imageUris.count() < 4) {
            AttachedImageItem(
                uri = imageUris[0].toUri(),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                showClose = false
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(2.dp,2.dp, 2.dp, 0.dp)) {
                    GridItem(uri = imageUris[0], modifier = Modifier.weight(1f))
                    GridItem(uri = imageUris[1], modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth().padding(2.dp,0.dp, 2.dp, 2.dp)) {
                    GridItem(uri = imageUris[2], modifier = Modifier.weight(1f))

                    // The 4th item with a potential overlay
                    Box(modifier = Modifier.weight(1f).clip(shape=shape)) {
                        GridItem(uri = imageUris[3], modifier = Modifier.fillMaxWidth())

                        if (imageUris.size > 4) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .matchParentSize()
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        shape = shape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${imageUris.size - 3}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridItem(uri: String, modifier: Modifier = Modifier) {
    AttachedImageItem(
        uri = uri.toUri(),
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        showClose = false
    )
}

@ThemePreviews
@Composable
fun PreviewImageCollectionUI() {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val mockImages = listOf(
        "android.resource://${context.packageName}/${R.drawable.ic_launcher_background}",
        "android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=3",
        "android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=2",
        "android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=4",
        "android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=5"
    )
    NoteThePadTheme(darkTheme = isDark) {
        Box(Modifier.fillMaxWidth()
            .background(NoteColors.resolveDisplayColor(NoteColors.colors[1].toArgb(), isDark))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                ImageCollectionUI(Modifier.padding(8.dp), mockImages)
            }
        }
    }
}