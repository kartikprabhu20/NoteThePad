package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.modify.components.AttachedImageItem
import com.mintanable.notethepad.feature_note.presentation.modify.components.MagicButton
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

fun LazyListScope.attachedImagesSection(
    images: List<Uri>,
    onRemoveImage: (Uri) -> Unit,
    onImageClick: (Uri) -> Unit,
    onAnalyzeImageClicked: () -> Unit,
    isAnalyzeImageSupported: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    if (images.isEmpty()) return

    item(key = "attached_images_section") {

        Box (
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

                with(animatedVisibilityScope) {
                    MagicButton(
                        title = stringResource(R.string.analyze_images),
                        isVisible = isAnalyzeImageSupported,
                        modifier = Modifier.fillMaxWidth()
                            .animateEnterExit(
                                enter = fadeIn(tween(300)),
                                exit = fadeOut(tween(100)) // Keep the exit fast
                            ),
                        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
                        onButtonClicked = onAnalyzeImageClicked,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }
    }
}

@SuppressLint("UseKtx")
@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
fun PreviewImageSection() {

    NoteThePadTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                val lazyListState = rememberLazyListState()
                val context = LocalContext.current
                val mockImages = listOf(
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}"),
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=3"),
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=2"),
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=4")
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NoteColors.colors[2])
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    attachedImagesSection(
                        images = mockImages,
                        onRemoveImage = { },
                        onImageClick = { },
                        isAnalyzeImageSupported = true,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility,
                        onAnalyzeImageClicked = {}
                    )
                }
            }
        }
    }
}

@SuppressLint("UseKtx")
@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
fun PreviewImageSectionNoMagic() {

    NoteThePadTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                val lazyListState = rememberLazyListState()
                val context = LocalContext.current
                val mockImages = listOf(
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}"),
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=3"),
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=2"),
                    Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher_background}?id=4")
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NoteColors.colors[2])
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    attachedImagesSection(
                        images = mockImages,
                        onRemoveImage = { },
                        onImageClick = { },
                        isAnalyzeImageSupported = false,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility,
                        onAnalyzeImageClicked = {}
                    )
                }
            }
        }
    }
}