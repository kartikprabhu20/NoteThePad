package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.core.richtext.model.SpanType
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlinx.coroutines.delay

@Composable
fun TextEditBar(
    activeStyles: Set<SpanType>,
    onStyleClick: (SpanType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wiggleAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(200)
        wiggleAnim.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 500
                0f at 0
                -20f at 100
                20f at 200
                -10f at 300
                10f at 400
                0f at 500
            }
        )
    }

    val wiggleModifier = Modifier.graphicsLayer { rotationZ = wiggleAnim.value }

    BottomAppBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Block types
                TextStyleButton(
                    label = "H1",
                    isSelected = SpanType.H1 in activeStyles,
                    onClick = { onStyleClick(SpanType.H1) },
                    modifier = wiggleModifier
                )
                TextStyleButton(
                    label = "H2",
                    isSelected = SpanType.H2 in activeStyles,
                    onClick = { onStyleClick(SpanType.H2) },
                    modifier = wiggleModifier
                )
                TextStyleButton(
                    label = "P",
                    isSelected = SpanType.PARAGRAPH in activeStyles,
                    onClick = { onStyleClick(SpanType.PARAGRAPH) },
                    modifier = wiggleModifier
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Inline styles
                IconStyleButton(
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    isSelected = SpanType.BOLD in activeStyles,
                    onClick = { onStyleClick(SpanType.BOLD) },
                    modifier = wiggleModifier
                )
                IconStyleButton(
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    isSelected = SpanType.ITALIC in activeStyles,
                    onClick = { onStyleClick(SpanType.ITALIC) },
                    modifier = wiggleModifier
                )
                IconStyleButton(
                    icon = Icons.Default.FormatUnderlined,
                    contentDescription = "Underline",
                    isSelected = SpanType.UNDERLINE in activeStyles,
                    onClick = { onStyleClick(SpanType.UNDERLINE) },
                    modifier = wiggleModifier
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Bullet
                IconStyleButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Bullet List",
                    isSelected = SpanType.BULLET in activeStyles,
                    onClick = { onStyleClick(SpanType.BULLET) },
                    modifier = wiggleModifier
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            // Close
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close formatting bar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TextStyleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        containerColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(40.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IconStyleButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        containerColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@ThemePreviews
@Composable
fun TextEditBarPreview() {
    NoteThePadTheme {
        TextEditBar(
            activeStyles = setOf(SpanType.BOLD, SpanType.H1),
            onStyleClick = {},
            onClose = {}
        )
    }
}


@ThemePreviews
@Composable
fun PreviewTextEditBarWithContent(){
    NoteThePadTheme {

        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "preview") { isVisible ->
                if (isVisible) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            contentWindowInsets = WindowInsets.systemBars,
                            containerColor = Color.Transparent,
                            bottomBar = {
                                TextEditBar(
                                    activeStyles = setOf(SpanType.BOLD, SpanType.H1),
                                    onStyleClick = {},
                                    onClose = {}
                                )
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NoteColors.colors[0])
                        ) { paddingValue ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(NoteColors.colors[1])
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = paddingValue,
                                ) {
                                    items(20) { index ->
                                        Text(
                                            "Note Content $index",
                                            Modifier.padding(16.dp).fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
