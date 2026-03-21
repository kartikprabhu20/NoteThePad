package com.mintanable.notethepad.feature_calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.core.common.Screen
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CalendarViewModel.UiEvent.NavigateToAddNote -> {
                    navController.navigate(
                        Screen.AddEditNoteScreen.passArgs(
                            reminderTime = event.reminderTime,
                            initialTitle = event.initialTitle
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource( R.string.calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // View mode tabs
            val tabs = CalendarViewMode.entries
            val selectedTabIndex = tabs.indexOf(state.viewMode)
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface, // Optional: customize background
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEach { mode ->
                    val selected = state.viewMode == mode
                    Tab(
                        selected = selected,
                        onClick = { viewModel.onEvent(CalendarEvent.ChangeViewMode(mode)) },
                        text = {
                            Text(
                                text = stringResource(mode.resId),
                                style = if (selected) {
                                    MaterialTheme.typography.titleSmall
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                }
                            )
                        }
                    )
                }
            }

            // Navigation row
            CalendarNavigationRow(
                label = when (state.viewMode) {
                    CalendarViewMode.MONTHLY -> state.currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    CalendarViewMode.WEEKLY -> {
                        val start = state.currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val end = start.plusDays(6)
                        "${start.format(DateTimeFormatter.ofPattern("MMM d"))} – ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                    }
                    CalendarViewMode.DAILY -> state.currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                },
                onPrevious = { viewModel.onEvent(CalendarEvent.NavigatePrevious) },
                onNext = { viewModel.onEvent(CalendarEvent.NavigateNext) }
            )

            when (state.viewMode) {
                CalendarViewMode.MONTHLY -> MonthlyView(
                    currentDate = state.currentDate,
                    selectedDay = state.selectedDay,
                    notesWithReminders = state.notesWithReminders,
                    onDayClick = { date ->
                        viewModel.onEvent(CalendarEvent.SelectDay(date))
                    },
                    onTimeSlotClick = { epochMillis ->
                        viewModel.onEvent(CalendarEvent.SelectTimeSlot(epochMillis))
                    },
                    onNoteClick = { note ->
                        navController.navigate(Screen.AddEditNoteScreen.passArgs(noteId = note.id))
                    }
                )
                CalendarViewMode.WEEKLY -> WeeklyView(
                    currentDate = state.currentDate,
                    notesWithReminders = state.notesWithReminders,
                    onTimeSlotClick = { epochMillis ->
                        viewModel.onEvent(CalendarEvent.SelectTimeSlot(epochMillis))
                    },
                    onNoteClick = { note ->
                        navController.navigate(Screen.AddEditNoteScreen.passArgs(noteId = note.id))
                    }
                )
                CalendarViewMode.DAILY -> DailyView(
                    currentDate = state.currentDate,
                    notesWithReminders = state.notesWithReminders,
                    onTimeSlotClick = { epochMillis ->
                        viewModel.onEvent(CalendarEvent.SelectTimeSlot(epochMillis))
                    },
                    onNoteClick = { note ->
                        navController.navigate(Screen.AddEditNoteScreen.passArgs(noteId = note.id))
                    }
                )
            }
        }
    }

    if (state.showNewNoteSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(CalendarEvent.DismissBottomSheet) },
            sheetState = sheetState
        ) {
            NewNoteBottomSheetContent(
                slotTime = state.selectedSlotTime,
                title = state.newNoteTitle,
                onTitleChange = { viewModel.onEvent(CalendarEvent.UpdateNewNoteTitle(it)) },
                onConfirm = { viewModel.onEvent(CalendarEvent.ConfirmNewNoteSlot) },
                onDismiss = { viewModel.onEvent(CalendarEvent.DismissBottomSheet) }
            )
        }
    }
}

@Composable
private fun CalendarNavigationRow(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}

@Composable
private fun MonthlyView(
    currentDate: LocalDate,
    selectedDay: LocalDate?,
    notesWithReminders: List<DetailedNote>,
    onDayClick: (LocalDate) -> Unit,
    onTimeSlotClick: (Long) -> Unit,
    onNoteClick: (DetailedNote) -> Unit
) {
    val firstDayOfMonth = currentDate.withDayOfMonth(1)
    val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
    // Start grid from Monday of the week containing the first day
    val gridStart = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val notesByDate = notesWithReminders.groupBy { note ->
        Instant.ofEpochMilli(note.reminderTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dow ->
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar grid — 6 weeks max
        val today = LocalDate.now()
        var gridDay = gridStart
        repeat(6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    val day = gridDay
                    val isCurrentMonth = day.month == currentDate.month
                    val isSelected = day == selectedDay
                    val isToday = day == today
                    val hasNotes = notesByDate.containsKey(day)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary)
                                else if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { onDayClick(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (hasNotes) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                            }
                        }
                    }
                    gridDay = gridDay.plusDays(1)
                }
            }
            if (gridDay > lastDayOfMonth && it >= 3) return@repeat
        }

        // Selected day agenda
        val effectiveDay = selectedDay ?: today
        val dayNotes = notesByDate[effectiveDay] ?: emptyList()

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = effectiveDay.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = {
                val epochMillis = effectiveDay.atTime(9, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                onTimeSlotClick(epochMillis)
            }) {
                Text("+ Add")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (dayNotes.isEmpty()) {
                item {
                    Text(
                        text = "No reminders for this day",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(dayNotes, key = { it.id }) { note ->
                    NoteReminderItem(note = note, onClick = { onNoteClick(note) })
                }
            }
        }
    }
}

@Composable
private fun WeeklyView(
    currentDate: LocalDate,
    notesWithReminders: List<DetailedNote>,
    onTimeSlotClick: (Long) -> Unit,
    onNoteClick: (DetailedNote) -> Unit
) {
    val weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val today = LocalDate.now()
    val hourLabelWidth = 40.dp

    // Group notes by (date, hour) for exact cell placement
    val notesByDateHour = notesWithReminders.groupBy { note ->
        val zdt = Instant.ofEpochMilli(note.reminderTime).atZone(ZoneId.systemDefault())
        Pair(zdt.toLocalDate(), zdt.hour)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Sticky day-header row
        stickyHeader {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(hourLabelWidth))
                    weekDays.forEach { day ->
                        val isToday = day == today
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .then(
                                        if (isToday) Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        // 24 hour rows × 7 day columns
        items(24) { hour ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Hour label
                Text(
                    text = "%02d:00".format(hour),
                    modifier = Modifier
                        .width(hourLabelWidth)
                        .defaultMinSize(minHeight = 52.dp)
                        .padding(top = 6.dp, end = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )

                // One cell per day
                weekDays.forEach { day ->
                    val cellNotes = notesByDateHour[Pair(day, hour)] ?: emptyList()
                    val slotEpochMillis = day.atTime(hour, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 52.dp)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(3.dp)
                            )
                            .then(
                                if (cellNotes.isEmpty()) Modifier.clickable { onTimeSlotClick(slotEpochMillis) }
                                else Modifier
                            )
                    ) {
                        if (cellNotes.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                cellNotes.forEach { note ->
                                    WeeklyNoteChip(note = note, onClick = { onNoteClick(note) })
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        }
    }
}

@Composable
private fun DailyView(
    currentDate: LocalDate,
    notesWithReminders: List<DetailedNote>,
    onTimeSlotClick: (Long) -> Unit,
    onNoteClick: (DetailedNote) -> Unit
) {
    val dayNotes = notesWithReminders.filter { note ->
        val noteDate = Instant.ofEpochMilli(note.reminderTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        noteDate == currentDate
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 24-hour slots
        items(24) { hour ->
            val slotEpochMillis = currentDate.atTime(hour, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val slotNotes = dayNotes.filter { note ->
                val noteHour = Instant.ofEpochMilli(note.reminderTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .hour
                noteHour == hour
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Hour label
                Text(
                    text = LocalDateTime.of(currentDate, LocalTime.of(hour, 0))
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                    modifier = Modifier.width(48.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (slotNotes.isEmpty()) {
                                Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onTimeSlotClick(slotEpochMillis) }
                            } else Modifier
                        )
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    slotNotes.forEach { note ->
                        NoteReminderItem(note = note, onClick = { onNoteClick(note) })
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun NoteReminderItem(
    note: DetailedNote,
    onClick: () -> Unit
) {
    val time = Instant.ofEpochMilli(note.reminderTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.width(36.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyNoteChip(
    note: DetailedNote,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = note.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewNoteBottomSheetContent(
    slotTime: Long,
    title: String,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedTime = if (slotTime > 0) {
        Instant.ofEpochMilli(slotTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm"))
    } else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "New Note",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (formattedTime.isNotBlank()) {
            Text(
                text = "Reminder: $formattedTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onConfirm,
                enabled = title.isNotBlank()
            ) {
                Text("Open Editor")
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private object CalendarPreviewData {
    val sampleNote = DetailedNote(
        id = 1,
        title = "Team Sync Meeting",
        content = "Discuss calendar feature",
        reminderTime = Instant.now().toEpochMilli(),
        timestamp = 123,
        color = NoteColors.colors[0].toArgb()
    )

    val notes = listOf(
        sampleNote,
        sampleNote.copy(id = 2, title = "Gym Session", reminderTime = Instant.now().plusSeconds(3600).toEpochMilli()),
        sampleNote.copy(id = 3, title = "Dinner with Sarah", reminderTime = Instant.now().plusSeconds(3600).toEpochMilli())
    )
}

@ThemePreviews
@Composable
fun CalendarNavigationRowPreview() {
    NoteThePadTheme {
        Surface {
            CalendarNavigationRow(
                label = "March 2026",
                onPrevious = {},
                onNext = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun MonthlyViewPreview() {
    NoteThePadTheme {
        Surface {
            MonthlyView(
                currentDate = LocalDate.now(),
                selectedDay = LocalDate.now(),
                notesWithReminders = CalendarPreviewData.notes,
                onDayClick = {},
                onTimeSlotClick = {},
                onNoteClick = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun WeeklyViewPreview() {
    NoteThePadTheme {
        Surface {
            WeeklyView(
                currentDate = LocalDate.now(),
                notesWithReminders = CalendarPreviewData.notes,
                onTimeSlotClick = {},
                onNoteClick = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun DailyViewPreview() {
    NoteThePadTheme {
        Surface {
            DailyView(
                currentDate = LocalDate.now(),
                notesWithReminders = CalendarPreviewData.notes,
                onTimeSlotClick = {},
                onNoteClick = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun NoteReminderItemPreview() {
    NoteThePadTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            NoteReminderItem(
                note = CalendarPreviewData.sampleNote,
                onClick = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun NewNoteBottomSheetPreview() {
    NoteThePadTheme {
        Surface {
            NewNoteBottomSheetContent(
                slotTime = Instant.now().toEpochMilli(),
                title = "My New Note",
                onTitleChange = {},
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun WeeklyNoteChipPreview() {
    NoteThePadTheme {
        Surface(modifier = Modifier.width(100.dp).padding(4.dp)) {
            WeeklyNoteChip(
                note = CalendarPreviewData.sampleNote,
                onClick = {}
            )
        }
    }
}
