package com.mintanable.notethepad.feature_note.presentation.modify.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.mintanable.notethepad.database.db.entity.AttachmentType
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.notes.util.AttachmentHelper
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun ZoomedImageOverlay(
    uri: Uri,
    playerEngine: ExoPlayer?,
    onClick: () -> Unit,
    imageSuggestions: List<String> = emptyList(),
    isAnalyzingImage: Boolean = false,
    imageQueryResult: String = "",
    isImageQueryLoading: Boolean = false,
    onSuggestionClicked: (String) -> Unit = {},
    onAppendToNote: (String) -> Unit = {},
    transitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val attachmentType = rememberSaveable(uri) { AttachmentHelper.getAttachmentType(context, uri) }
    val isImage = attachmentType != AttachmentType.VIDEO

    // Back gesture / button closes overlay
    BackHandler { onClick() }

    with(transitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .pointerInput(Unit) { detectTapGestures { /* consume all taps */ } }
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (attachmentType == AttachmentType.VIDEO) Modifier.aspectRatio(1f)
                            else Modifier
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "image-${uri}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(400) }
                        )
                ) {
                    if (attachmentType == AttachmentType.VIDEO) {
                        VideoPlayerUI(playerEngine)
                    } else {
                        ZoomableImage(
                            uri = uri,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)
                        )
                    }
                }

                // Image analysis UI (only for images, not videos)
                if (isImage) {
                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        // Loading analysis
                        isAnalyzingImage -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }

                        // Show query result (replaces suggestions)
                        isImageQueryLoading || imageQueryResult.isNotEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isImageQueryLoading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (imageQueryResult.isNotEmpty()) {
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .padding(top = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = imageQueryResult,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onAppendToNote(imageQueryResult) },
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(stringResource(R.string.btn_add_to_note))
                                    }
                                }
                            }
                        }

                        // Show suggestion buttons
                        imageSuggestions.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(imageSuggestions) { suggestion ->
                                    MagicButton(
                                        title = suggestion,
                                        isVisible = true,
                                        onButtonClicked = { onSuggestionClicked(suggestion) },
                                        sharedTransitionScope = transitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Close button — top-end corner
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .transformable(state = transformableState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double-tap to toggle zoom
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            },
        contentScale = ContentScale.Fit
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
fun ZoomedImageOverlaySuggestionsPreview() {
    NoteThePadTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {
                    ZoomedImageOverlay(
                        uri = Uri.EMPTY,
                        playerEngine = null,
                        onClick = {},
                        imageSuggestions = listOf("Summarize", "Extract Text", "Describe"),
                        transitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
fun ZoomedImageOverlayResultPreview() {
    NoteThePadTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {
                    ZoomedImageOverlay(
                        uri = Uri.EMPTY,
                        playerEngine = null,
                        onClick = {},
                        imageQueryResult = "This is a sample analysis result from the AI model. It can contain multiple lines of text to describe the content of the image.",
                        transitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
fun ZoomedImageOverlayLoadingPreview() {
    NoteThePadTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {
                    ZoomedImageOverlay(
                        uri = Uri.EMPTY,
                        playerEngine = null,
                        onClick = {},
                        isAnalyzingImage = true,
                        transitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
            }
        }
    }
}
