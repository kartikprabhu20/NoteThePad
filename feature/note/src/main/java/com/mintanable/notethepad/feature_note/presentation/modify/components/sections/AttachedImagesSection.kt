package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.presentation.modify.components.AttachedImageItem

fun LazyListScope.attachedImagesSection(
    images: List<Uri>,
    onRemoveImage: (Uri) -> Unit,
    onImageClick: (Uri) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    if (images.isEmpty()) return

    item(key = "attached_images_section") {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = images,
                        key = { uri -> "row-${uri}" }
                    ) { uri ->
                        with(sharedTransitionScope) {
                            AttachedImageItem(
                                uri = uri,
                                onDelete = { onRemoveImage(it) },
                                onClick = { onImageClick(it) },
                                modifier = Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "image-$uri"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}