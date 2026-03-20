package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import android.net.Uri
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mintanable.notethepad.core.model.note.Attachment
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.model.note.MediaState
import com.mintanable.notethepad.core.model.note.Tag
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.NoteTextFieldState
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteEvent
import com.mintanable.notethepad.feature_note.presentation.modify.components.MagicButton
import com.mintanable.notethepad.feature_note.presentation.modify.components.NoteBottomAppBar
import com.mintanable.notethepad.feature_note.presentation.modify.components.SuggestedTagsRow
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.feature_note.presentation.notes.components.TransparentHintTextField
import kotlinx.coroutines.launch

@Composable
fun NoteEditorContent(
    noteId: Long,
    noteColor: Int,
    attachedImages: List<Uri>,
    titleState: NoteTextFieldState,
    contentState: NoteTextFieldState,
    isCheckboxListAvailable: Boolean,
    checkListItems: List<CheckboxItem>,
    attachedAudios: List<Attachment>,
    mediaState: MediaState?,
    reminderTime: Long,
    tags: List<Tag> = emptyList(),
    suggestedTags: List<String> = emptyList(),
    isSuggestionTagsLoading: Boolean = false,
    onEvent: (AddEditNoteEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    var activeDragUnCheckIndex by remember { mutableStateOf<String?>(null) }
    var activeDragCheckIndex by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val noteBackgroundAnimatable = remember {
        Animatable(if (noteColor == -1) Color.White else Color(noteColor))
    }
    LaunchedEffect(noteColor) {
        if (noteColor != -1) {
            scope.launch {
                noteBackgroundAnimatable.animateTo(
                    targetValue = Color(noteColor),
                    animationSpec = tween(500)
                )
            }
        }
    }

    val checklistCharCount by remember(checkListItems) { derivedStateOf { checkListItems.sumOf { it.text.length } } }
    val charCount = titleState.text.length + contentState.text.length + checklistCharCount
    val showMagicButton = charCount > 400 && !isSuggestionTagsLoading

    with(sharedTransitionScope) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = Color.Transparent,
            bottomBar = {
                CompositionLocalProvider(LocalAbsoluteTonalElevation provides 0.dp) {
                    NoteBottomAppBar(
                        utilityButtons = listOf(
                            Triple(Icons.Default.AttachFile, BottomSheetType.ATTACH, stringResource(R.string.content_description_attach)),
                            Triple(
                                if (isCheckboxListAvailable) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                BottomSheetType.CHECKBOX,
                                stringResource(R.string.content_description_checkbox)
                            ),
                            Triple(
                                Icons.Default.NotificationAdd,
                                BottomSheetType.REMINDER,
                                stringResource(R.string.content_description_reminders)
                            ),
                            Triple(
                                Icons.Default.MoreHoriz,
                                BottomSheetType.MORE_SETTINGS,
                                stringResource(R.string.content_description_settings)
                            )
                        ),
                        modifier = Modifier,
                        onActionClick = { sheetType ->

                            if (sheetType == BottomSheetType.CHECKBOX) {
                                onEvent(AddEditNoteEvent.ToggleCheckbox)
                            } else {
                                onEvent(AddEditNoteEvent.UpdateSheetType(sheetType))
                            }
                        },
                        onSaveClick = { onEvent(AddEditNoteEvent.SaveNote) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }

            },
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                MagicButton(
                    title = stringResource(R.string.btn_auto_tag),
                    isVisible = showMagicButton,
                    modifier = Modifier,
                    onButtonClicked = { onEvent(AddEditNoteEvent.ShowSuggestions)},
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        ) { paddingValue ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .background(noteBackgroundAnimatable.value)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = if (noteId <= 0L) "notescreens_fab" else "note-$noteId"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(300)),
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
//                    .renderInSharedTransitionScopeOverlay(
//                        zIndexInOverlay = if (animatedVisibilityScope.transition.isRunning) 2f else 0f
//                    )
            ) {

                val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = paddingValue
                ) {
                    colorSelectorSection(
                        selectedColor = noteColor,
                        onColorClick = { colorInt -> onEvent(AddEditNoteEvent.ChangeColor(colorInt)) }
                    )

                    item {
                        SuggestedTagsRow(
                            suggestions = suggestedTags,
                            isLoading = isSuggestionTagsLoading,
                            onTagAccepted = { tag -> onEvent(AddEditNoteEvent.InsertLabel(tag)) },
                            onDismiss = { onEvent(AddEditNoteEvent.ClearSuggestions) }
                        )
                    }

                    attachedImagesSection(
                        images = attachedImages,
                        onRemoveImage = { deletedUri ->
                            onEvent(
                                AddEditNoteEvent.RemoveImage(
                                    deletedUri
                                )
                            )
                        },
                        onImageClick = { onEvent(AddEditNoteEvent.ToggleZoom(it)) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TransparentHintTextField(
                            text = titleState.text,
                            hint = titleState.hint,
                            onValueChange = { onEvent(AddEditNoteEvent.EnteredTitle(it)) },
                            onFocusChange = { onEvent(AddEditNoteEvent.ChangeTitleFocus(it)) },
                            isHintVisible = titleState.isHintVisible,
                            isSingleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .sharedBounds(
                                    sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                        key = "note-title-${noteId}"
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ ->
                                        tween()
                                    },
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                ),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isCheckboxListAvailable) {
                            TransparentHintTextField(
                                text = contentState.text,
                                hint = contentState.hint,
                                onValueChange = { onEvent(AddEditNoteEvent.EnteredContent(it)) },
                                onFocusChange = { onEvent(AddEditNoteEvent.ChangeContentFocus(it)) },
                                isHintVisible = contentState.isHintVisible,
                                isSingleLine = false,
                                textStyle = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .sharedBounds(
                                        sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                            key = "note-content-${noteId}"
                                        ),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ ->
                                            tween()
                                        },
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                    )
                            )
                        }
                    }

                    if (isCheckboxListAvailable) {
                        checkboxListSection(
                            activeDragCheckIndex = activeDragCheckIndex,
                            activeDragUnCheckIndex = activeDragUnCheckIndex,
                            items = checkListItems,
                            focusRequesters = focusRequesters,
                            onEnterPressed = { previousCheckItem -> onEvent(AddEditNoteEvent.AddChecklistItem(previousCheckItem)) },
                            onItemChanged = { updatedItem ->
                                onEvent(AddEditNoteEvent.UpdateCheckList(checkListItems.map { if (it.id == updatedItem.id) updatedItem else it }))
                            },
                            onListOrderUpdated = { orderedList ->
                                onEvent(
                                    AddEditNoteEvent.UpdateCheckList(
                                        orderedList
                                    )
                                )
                            },
                            onDragStateChangedChecked = {
                                activeDragCheckIndex = it
                            },
                            onDragStateChangedUnChecked = {
                                activeDragUnCheckIndex = it
                            }
                        )
                    }

                    audioAttachmentSection(
                        attachedAudios = attachedAudios,
                        mediaState = mediaState,
                        onDelete = { deletedUri -> onEvent(AddEditNoteEvent.RemoveAudio(deletedUri)) },
                        onPlayPause = { uri -> onEvent(AddEditNoteEvent.UpdateNowPlaying(uri)) }
                    )

                    reminderAttachmentSection(
                        reminderTime = reminderTime,
                        onDelete = { onEvent(AddEditNoteEvent.CancelReminder) },
                        onClick = { onEvent(AddEditNoteEvent.CheckAlarmPermission) }
                    )

                    tagsSection(
                        tags = tags,
                        onDelete = { tagName -> onEvent(AddEditNoteEvent.DeleteLabel(tagName)) },
                    )
                }
            }
        }
    }
}