package com.mintanable.notethepad.feature_settings.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.components.drawNoteShape
import com.mintanable.notethepad.core.model.settings.NoteShape
import com.mintanable.notethepad.feature_settings.R
import com.mintanable.notethepad.feature_settings.presentation.components.BreathingLogo
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    isDarkTheme: Boolean,
    onComplete: () -> Unit
) {
    val pageCount = 6
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()

    val bgColor = NoteColors.resolveDisplayColor(NoteColors.colors[pagerState.currentPage+1].toArgb(), isDarkTheme)
    val animatedBgColor = animateColorAsState(targetValue = bgColor, label = "bg")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor.value)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage(isDarkTheme)
                1 -> AgenticAiPage()
                2 -> MultimediaPage()
                3 -> CloudSyncPage()
                4 -> ThemingPage(isDarkTheme)
                5 -> CalendarWidgetsPage()
            }
        }

        // Skip button (top-right, not on last page)
        if (pagerState.currentPage < pageCount - 1) {
            TextButton(
                onClick = onComplete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Text(
                    "Skip",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Bottom section: page indicators + button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(pageCount) { index ->
                    val width = animateDpAsState(
                        targetValue = if (index == pagerState.currentPage) 24.dp else 8.dp,
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width.value)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Next / Get Started button
            Button(
                onClick = {
                    if (pagerState.currentPage < pageCount - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < pageCount - 1) "Next" else "Get Started",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── Page 1: Welcome ─────────────────────────────────────────────────────

@Composable
private fun WelcomePage(isDarkTheme: Boolean) {

    val painter = painterResource(id = R.drawable.notethepad)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Canvas(modifier = Modifier.size(120.dp)) {
            drawNoteShape(NoteShape.PINNED_NOTE, NoteColors.colorPairs[6].light.toArgb())

            val iconSize = size * 0.8f
            translate(
                left = (size.width - iconSize.width) / 2,
                top = (size.height - iconSize.height) / 2
            ) {
                with(painter) {
                    draw(
                        size = iconSize,
                        alpha = 0.8f,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        AnimatedContent(
            targetState = true,
            label = "InfiniteLogoToSearch"
        ) { isShowing ->

            if(isShowing)
            BreathingLogo(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your intelligent note-taking companion powered by on-device AI",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// ── Page 2: Agentic AI ──────────────────────────────────────────────────

@Composable
private fun AgenticAiPage() {
    OnboardingPageLayout(
        icon = Icons.Filled.AutoAwesome,
        title = "Agentic AI",
        subtitle = "AI that acts on your behalf"
    ) {
        FeatureBullet("Auto-Tagging", "Analyzes content and assigns labels automatically")
        FeatureBullet("Smart Summaries", "Generates concise summaries from text, audio, and images")
        FeatureBullet("Agentic Reminders", "Detects \"remind me tomorrow\" and schedules alarms autonomously")
        FeatureBullet("On-Device Privacy", "Gemma models run locally - your data never leaves your phone")

        Spacer(modifier = Modifier.height(8.dp))

        // Mini demo: show AI model tiers
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Supported Models",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModelTierRow("Gemini 3 Flash", "Cloud")
                ModelTierRow("Gemini Nano", "On-device")
                ModelTierRow("Gemma (LiteRT)", "Downloadable")
            }
        }
    }
}

@Composable
private fun ModelTierRow(name: String, tier: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            tier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

// ── Page 3: Rich Multimedia ─────────────────────────────────────────────

@Composable
private fun MultimediaPage() {
    OnboardingPageLayout(
        icon = Icons.Filled.Mic,
        title = "Rich Multimedia",
        subtitle = "More than just text"
    ) {
        FeatureBullet("Rich Attachments", "Attach photos, audio, and video directly to notes")
        FeatureBullet("Audio Transcription", "Record voice memos and get automatic text transcription")
        FeatureBullet("Image Analysis", "AI describes your images and suggests actions")
        FeatureBullet("OCR", "Extract text from photos of documents and handwriting")
        FeatureBullet("Image Query", "Ask questions about any image with streaming AI responses")
        FeatureBullet("Rich Text", "Customize the texts with different styles, headers, paragrap and the classics: bold, italic, underline")
    }
}

// ── Page 4: Cloud & Sync ────────────────────────────────────────────────

@Composable
private fun CloudSyncPage() {
    OnboardingPageLayout(
        icon = Icons.Filled.Cloud,
        title = "Cloud & Sync",
        subtitle = "Your notes, everywhere"
    ) {
        FeatureBullet("Google Drive Backup", "Automated daily, weekly, or monthly backups")
        FeatureBullet("SupaSync", "Real-time cloud sync across devices")
        FeatureBullet("Media Backup", "Optionally include images, audio, and video")
        FeatureBullet("Secure", "Encrypted storage with Google Tink")
    }
}

// ── Page 5: Theming ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemingPage(isDarkTheme: Boolean) {
    OnboardingPageLayout(
        icon = Icons.Filled.Palette,
        title = "Beautiful Theming",
        subtitle = "Make it yours"
    ) {
        // Live demo: Color palette
        Text(
            "11 Note Colors",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
//        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 6
        ) {
            NoteColors.colorPairs.forEach { pair ->
                val color = if (isDarkTheme) pair.dark else pair.light
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "12 Background images",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
//        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 6
        ) {
            NoteColors.backgroundImages.forEach { img ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(28.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = img.res),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live demo: Note shapes
        Text(
            "13 Note Shapes",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
//        Spacer(modifier = Modifier.height(8.dp))

        val demoShapes = listOf(
            NoteShape.PINNED_NOTE,
            NoteShape.PERFORATED_PAPER,
            NoteShape.STICKY_CLIPPED,
            NoteShape.TAPED_NOTE,
            NoteShape.SCALLOPED_EDGE,
        )
        val demoColors = listOf(
            NoteColors.colorPairs[1],
            NoteColors.colorPairs[3],
            NoteColors.colorPairs[4],
            NoteColors.colorPairs[7],
            NoteColors.colorPairs[9],
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            demoShapes.forEachIndexed { index, shape ->
                val color = NoteColors.resolveDisplayColor(demoColors[index].light.toArgb(), isDarkTheme)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawNoteShape(shape, color.toArgb())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Light & dark mode with 12 background patterns",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Page 6: Calendar & Widgets ──────────────────────────────────────────

@Composable
private fun CalendarWidgetsPage() {
    OnboardingPageLayout(
        icon = Icons.Filled.CalendarMonth,
        title = "Calendar & Widgets",
        subtitle = "Stay organized"
    ) {
        FeatureBullet("Calendar View", "Browse notes by date with a visual calendar")
        FeatureBullet("Reminders", "Set per-note reminders with alarm notifications")
        FeatureBullet("Home Widgets", "Pin single notes or recent notes list to your home screen")
        FeatureBullet("Quick Capture", "Create notes directly from widgets")

        Spacer(modifier = Modifier.height(16.dp))

        // Mini widget preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            WidgetPreview(
                icon = Icons.Filled.Edit,
                label = "Single Note"
            )
            WidgetPreview(
                icon = Icons.Filled.Widgets,
                label = "Notes List"
            )
        }
    }
}

@Composable
private fun WidgetPreview(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(width = 100.dp, height = 80.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Shared Components ───────────────────────────────────────────────────

@Composable
private fun OnboardingPageLayout(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 100.dp, bottom = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(36.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun FeatureBullet(title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────

@ThemePreviews
@Composable
fun PreviewOnboardingScreen() {
    NoteThePadTheme {
        OnboardingScreen(isDarkTheme = isSystemInDarkTheme(), onComplete = {})
    }
}

@ThemePreviews
@Composable
fun PreviewWelcomePage() {
    NoteThePadTheme {
        Surface {
            WelcomePage(isDarkTheme = isSystemInDarkTheme())
        }
    }
}

@ThemePreviews
@Composable
fun PreviewAgenticAiPage() {
    NoteThePadTheme {
        Surface {
            AgenticAiPage()
        }
    }
}

@ThemePreviews
@Composable
fun PreviewMultimediaPage() {
    NoteThePadTheme {
        Surface {
            MultimediaPage()
        }
    }
}

@ThemePreviews
@Composable
fun PreviewCloudSyncPage() {
    NoteThePadTheme {
        Surface {
            CloudSyncPage()
        }
    }
}

@ThemePreviews
@Composable
fun PreviewThemingPage() {
    NoteThePadTheme {
        Surface {
            ThemingPage(isDarkTheme = isSystemInDarkTheme())
        }
    }
}

@ThemePreviews
@Composable
fun PreviewCalendarWidgetsPage() {
    NoteThePadTheme {
        Surface {
            CalendarWidgetsPage()
        }
    }
}

@ThemePreviews
@Composable
fun PreviewWidgetPreview() {
    NoteThePadTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            WidgetPreview(icon = Icons.Default.Edit, label = "Single Note")
        }
    }
}

@ThemePreviews
@Composable
fun PreviewFeatureBullet() {
    NoteThePadTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            FeatureBullet(title = "Smart Summaries", description = "Generates concise summaries from text, audio, and images")
        }
    }
}
