package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.mintanable.notethepad.MainActivity
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.domain.util.CheckboxConvertors
import com.mintanable.notethepad.feature_note.presentation.notes.util.ReminderReceiver.Companion.LAUNCH_EDIT_SCREEN
import com.mintanable.notethepad.feature_note.presentation.notes.util.ReminderReceiver.Companion.TARGET_NOTE_ID
import com.mintanable.notethepad.feature_widgets.presentation.components.IconsRow
import com.mintanable.notethepad.feature_widgets.presentation.components.RoundedScrollingLazyVerticalGrid
import com.mintanable.notethepad.feature_widgets.presentation.components.WidgetTitleBar
import com.mintanable.notethepad.feature_widgets.presentation.utils.GridBreakpointPreviews
import com.mintanable.notethepad.feature_widgets.presentation.utils.LargeWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.MediumWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.NoteListLayoutTextStyles
import com.mintanable.notethepad.feature_widgets.presentation.utils.NotesListDimensions.gridCells
import com.mintanable.notethepad.feature_widgets.presentation.utils.RefreshNoteWidgetCallback
import com.mintanable.notethepad.feature_widgets.presentation.utils.SmallWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors

class NoteListWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val noteUseCases = EntryPointAccessors.fromApplication(
            context, WidgetEntryPoint::class.java
        ).noteUseCases()

        provideContent {

            val notes by noteUseCases.getTopNotes(10)
                .collectAsState(initial = emptyList())

            GlanceTheme {
                WidgetContent(notes)
            }
        }
    }
}

@Composable
fun WidgetContent(
    items: List<DetailedNote>,
) {
    Scaffold(
        titleBar = {
            WidgetTitleBar(
                title = "Recent Notes",
                titleIconRes = R.mipmap.notethepad_launcher_round,
                titleBarActionIconRes = R.drawable.baseline_refresh_24,
                titleBarActionIconContentDescription = "refresh widgets",
            ) {
                actionRunCallback<RefreshNoteWidgetCallback>()
            }
        },
        backgroundColor = GlanceTheme.colors.widgetBackground,
        horizontalPadding = 4.dp,
        modifier = GlanceModifier.padding(bottom = 12.dp)
    ) {
        Grid(items)
    }
}

@Composable
private fun Grid(items: List<DetailedNote>) {

    Box(modifier = GlanceModifier.fillMaxSize()) {

        RoundedScrollingLazyVerticalGrid(
            modifier = GlanceModifier.fillMaxSize(),
            gridCells = gridCells,
            items = items,
            itemContentProvider = { item ->
                NoteItemRow(item)
            },
            cellSpacing = 2.dp
        )

        //The Floating Action Button
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd // Positions it at the bottom right
        ) {
            val context = LocalContext.current
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(LAUNCH_EDIT_SCREEN, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            Box(
                modifier = GlanceModifier
                    .size(56.dp)
                    .background(
                        imageProvider = ImageProvider(R.drawable.widget_fab_background),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary))
                    .clickable(actionStartActivity(intent)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.baseline_add_24),
                    contentDescription = "Add Note",
                    modifier = GlanceModifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun NoteItemRow(note: DetailedNote) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(TARGET_NOTE_ID, note.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val estimatedHeight = rememberNoteHeight(note)

    var containerModifier = GlanceModifier
        .fillMaxWidth()
        .padding(vertical = 4.dp, horizontal = 8.dp)

    if(gridCells==1) {
        containerModifier = containerModifier.height(estimatedHeight)
    }

    Box(
        modifier = containerModifier
    ) {

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(
                    imageProvider = ImageProvider(R.drawable.widget_background),
                    colorFilter = ColorFilter.tint(ColorProvider(Color(note.color)))
                )
                .padding(8.dp)
                .clickable(actionStartActivity(intent))
        ) {
            Text(
                text = note.title,
                style = NoteListLayoutTextStyles.titleText,
                maxLines = 1
            )
            Spacer(GlanceModifier.height(4.dp))
            IconsRow(note)
            Spacer(GlanceModifier.height(4.dp))

            Text(
                text = note.content,
                style = NoteListLayoutTextStyles.contentText,
                maxLines = 5
            )
        }
    }
}

@Composable
fun rememberNoteHeight(note: DetailedNote): Dp {
    val titleHeight = 20.dp
    val paddingTotal = 12.dp + 12.dp + 4.dp + 4.dp  // top + bottom padding + spacers

    val contentHeight = if (note.content.isBlank()) {
        0.dp
    } else {
        val charsPerLine = 38  // approx for 12sp in typical widget width
        val lines = (note.content.length / charsPerLine + 1).coerceIn(1, 5)
        (lines * 18).dp
    }

    val hasIcons = note.imageUris.isNotEmpty() ||
            note.audioAttachments.isNotEmpty() ||
            note.reminderTime > -1 ||
            CheckboxConvertors.isContentCheckboxList(note.content)
    val iconsHeight = if (hasIcons) 28.dp else 0.dp
    val iconSpacerHeight = if (hasIcons) 4.dp else 0.dp

    return titleHeight + paddingTotal + contentHeight + iconsHeight + iconSpacerHeight
}

@GridBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun NoteListContentPreview() {
    WidgetContent(
        listOf(
            DetailedNote(
                title = "Remember to call landlord",
                content = "Repairing basin and pipes",
                timestamp = 123,
                color = NoteColors.colors[1].toArgb(),
                reminderTime = 1234556
            ),
            DetailedNote(
                title = "Remember to call landlord",
                content = "Repairing basin and pipes",
                timestamp = 123,
                color = NoteColors.colors[2].toArgb()
            ),

            DetailedNote(
                title = "Remember to call landlord",
                content = "Repairing basin and pipes",
                timestamp = 123,
                color = NoteColors.colors[4].toArgb(),
                reminderTime = 1234556
            ),
            DetailedNote(
                title = "Remember to call landlord",
                content = "Repairing basin and pipes",
                timestamp = 123,
                color = NoteColors.colors[2].toArgb()
            ),

            DetailedNote(
                title = "Remember to call landlord",
                content = "Repairing basin and pipes",
                timestamp = 123,
                color = NoteColors.colors[1].toArgb()
            ),
            DetailedNote(
                title = "Remember to call landlord",
                content = "Repairing basin and pipes",
                timestamp = 123,
                color = NoteColors.colors[3].toArgb(),
                reminderTime = 1234556
            )
        )
    )
}
