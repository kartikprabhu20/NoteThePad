package com.mintanable.notethepad.feature_note.presentation.modify.components.sections

import android.net.Uri
import com.mintanable.notethepad.core.model.ai.AiCapabilities
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mintanable.notethepad.NoteColors
import com.mintanable.notethepad.core.richtext.model.RichTextState
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.common.FeatureFlags
import com.mintanable.notethepad.core.model.collaboration.Collaborator
import com.mintanable.notethepad.core.model.note.MediaState
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.NoteTextFieldState
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteEvent
import com.mintanable.notethepad.feature_note.presentation.modify.components.MagicButton
import com.mintanable.notethepad.feature_note.presentation.modify.components.NoteBottomAppBar
import com.mintanable.notethepad.feature_note.presentation.modify.components.SuggestedTagsRow
import com.mintanable.notethepad.feature_note.presentation.modify.components.TextEditBar
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.feature_note.presentation.notes.components.TransparentHintTextField

@Composable
fun NoteEditorContent(
    noteId: String,
    noteColor: Int,
    backgroundImage: Int,
    attachedImages: List<Uri>,
    titleState: NoteTextFieldState,
    contentState: NoteTextFieldState,
    contentRichTextState: RichTextState,
    isRichTextBarActive: Boolean,
    isCheckboxListAvailable: Boolean,
    checkListItems: List<CheckboxItem>,
    attachedAudios: List<Attachment>,
    mediaState: MediaState?,
    transcribingUri: String?,
    reminderTime: Long,
    tagEntities: List<TagEntity> = emptyList(),
    suggestedTags: List<String> = emptyList(),
    isSuggestionTagsLoading: Boolean = false,
    collaborators: List<Collaborator> = emptyList(),
    aiCapabilities: AiCapabilities = AiCapabilities.NONE,
    summary: String = "",
    onEvent: (AddEditNoteEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isDarkTheme: Boolean = false
) {
    var activeDragUnCheckIndex by remember { mutableStateOf<String?>(null) }
    var activeDragCheckIndex by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val resolvedColor = NoteColors.resolveDisplayColor(noteColor, isDarkTheme)
    val noteBackgroundAnimatable = remember {
        Animatable(if (noteColor == -1) Color.Transparent else resolvedColor)
    }
    LaunchedEffect(noteColor, isDarkTheme) {
        val target = if (noteColor == -1) Color.Transparent else resolvedColor
        noteBackgroundAnimatable.animateTo(target, tween(500))
    }

    val resolvedBackgroundRes = NoteColors.resolveBackgroundImage(backgroundImage)
    val hasBackgroundImage = backgroundImage != -1
    val imageAlpha by animateFloatAsState(
        targetValue = if (hasBackgroundImage) 1f else 0f,
        animationSpec = tween(500),
        label = "image_fade"
    )

    val checklistCharCount by remember(checkListItems) {
        derivedStateOf { checkListItems.sumOf { it.text.length } }
    }
    val charCount =
        titleState.richText.rawText.length + contentRichTextState.document.rawText.length + checklistCharCount
    val showMagicButton = aiCapabilities.canAutoTag && charCount > 400 && !isSuggestionTagsLoading
    val showSummarizeButton = aiCapabilities.canSummarize && charCount > 100

    val lazyListState = rememberLazyListState()

    LaunchedEffect(isSuggestionTagsLoading, suggestedTags) {
        if (isSuggestionTagsLoading || suggestedTags.isNotEmpty()) {
            lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
        }
    }

    val extraBottomPadding by animateDpAsState(
        targetValue = when {
            showMagicButton || showSummarizeButton -> 56.dp
            else -> 0.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "fab_padding_animation"
    )

    // Local TextFieldValue state for the title (plain text, no rich text)
    var titleTFV by remember { mutableStateOf(TextFieldValue(titleState.richText.rawText)) }
    LaunchedEffect(titleState.richText.rawText) {
        if (titleTFV.text != titleState.richText.rawText) {
            titleTFV = TextFieldValue(titleState.richText.rawText)
        }
    }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)

    with(sharedTransitionScope) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = Color.Transparent,
            bottomBar = {
                CompositionLocalProvider(LocalAbsoluteTonalElevation provides 0.dp) {
                    Box(modifier = Modifier.imePadding()) {
                        AnimatedContent(
                            targetState = isRichTextBarActive,
                            label = "BottomBarSwitch"
                        ) { richTextActive ->
                            if (richTextActive) {
                                TextEditBar(
                                    activeStyles = contentRichTextState.activeStyles,
                                    onStyleClick = { onEvent(AddEditNoteEvent.ApplyContentFormat(it)) },
                                    onClose = { onEvent(AddEditNoteEvent.ToggleRichTextBar) }
                                )
                            } else {
                                NoteBottomAppBar(
                                    utilityButtons = listOf(
                                        Triple(Icons.Default.AttachFile, BottomSheetType.ATTACH, stringResource(R.string.content_description_attach)),
                                        Triple(Icons.Default.FormatPaint, BottomSheetType.COLOR_SELECTOR, stringResource(R.string.content_description_color_selector)),
                                        Triple(
                                            if (isCheckboxListAvailable) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            BottomSheetType.CHECKBOX,
                                            stringResource(R.string.content_description_checkbox)
                                        ),
                                        Triple(Icons.Default.NotificationAdd, BottomSheetType.REMINDER, stringResource(R.string.content_description_reminders)),
                                        Triple(Icons.Default.MoreHoriz, BottomSheetType.MORE_SETTINGS, stringResource(R.string.content_description_settings))
                                    ),
                                    modifier = Modifier,
                                    isRichTextEnabled = contentState.isFocused,
                                    onActionClick = { sheetType ->
                                        if (sheetType == BottomSheetType.CHECKBOX) {
                                            onEvent(AddEditNoteEvent.ToggleCheckbox)
                                        } else {
                                            onEvent(AddEditNoteEvent.UpdateSheetType(sheetType))
                                        }
                                    },
                                    onRichTextClick = { onEvent(AddEditNoteEvent.ToggleRichTextBar) },
                                    onSaveClick = { onEvent(AddEditNoteEvent.SaveNote) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    MagicButton(
                        title = stringResource(R.string.btn_auto_tag),
                        isVisible = showMagicButton,
                        modifier = Modifier,
                        onButtonClicked = { onEvent(AddEditNoteEvent.ShowSuggestions) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )

                    MagicButton(
                        title = stringResource(R.string.btn_summarize),
                        isVisible = showSummarizeButton,
                        onButtonClicked = { onEvent(AddEditNoteEvent.SummarizeNote) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        ) { paddingValue ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .background(noteBackgroundAnimatable.value)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = if (noteId.isEmpty()) "notescreens_fab" else "note-$noteId"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(300)),
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
            ) {

                if (hasBackgroundImage) {
                    Image(
                        painter = painterResource(id = resolvedBackgroundRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = imageAlpha)
                    )
                }
                val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),

                    contentPadding = PaddingValues(
                        top = paddingValue.calculateTopPadding(),
                        start = paddingValue.calculateStartPadding(LayoutDirection.Ltr),
                        end = paddingValue.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = paddingValue.calculateBottomPadding() + extraBottomPadding + 16.dp
                    )
                ) {
                    attachedImagesSection(
                        images = attachedImages,
                        onRemoveImage = { deletedUri ->
                            onEvent(AddEditNoteEvent.RemoveImage(deletedUri))
                        },
                        onImageClick = { onEvent(AddEditNoteEvent.ToggleZoom(it)) },
                        isAnalyzeImageSupported = aiCapabilities.canAnalyzeImage,
                        onAnalyzeImageClicked = { onEvent(AddEditNoteEvent.ToggleZoom(attachedImages[0])) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TransparentHintTextField(
                            value = titleTFV,
                            hint = titleState.hint,
                            onValueChange = { v ->
                                titleTFV = v
                                onEvent(AddEditNoteEvent.EnteredTitle(v.text))
                            },
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
                                    boundsTransform = { _, _ -> tween() },
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                ),
                        )
                    }

                    if (summary.isNotEmpty()) {
                        summarySection(
                            summary = summary,
                            onEvent = onEvent,
                            onDelete = { onEvent(AddEditNoteEvent.DeleteSummary) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isCheckboxListAvailable) {
                            TransparentHintTextField(
                                value = contentRichTextState.textFieldValue,
                                hint = contentState.hint,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .sharedBounds(
                                        sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                            key = "note-content-${noteId}"
                                        ),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ -> tween() },
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                    ),
                                isHintVisible = contentState.isHintVisible,
                                onValueChange = { onEvent(AddEditNoteEvent.EnteredContent(it)) },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                onFocusChange = { onEvent(AddEditNoteEvent.ChangeContentFocus(it)) },
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
                                onEvent(AddEditNoteEvent.UpdateCheckList(orderedList))
                            },
                            onDragStateChangedChecked = { activeDragCheckIndex = it },
                            onDragStateChangedUnChecked = { activeDragUnCheckIndex = it }
                        )
                    }

                    audioAttachmentSection(
                        attachedAudios = attachedAudios,
                        mediaState = mediaState,
                        transcribingUri = transcribingUri,
                        onDelete = { deletedUri -> onEvent(AddEditNoteEvent.RemoveAudio(deletedUri)) },
                        onPlayPause = { uri -> onEvent(AddEditNoteEvent.UpdateNowPlaying(uri)) },
                        onTranscribe = { uri -> onEvent(AddEditNoteEvent.TranscribeAttachedAudio(uri)) },
                        isTranscribeSupported = aiCapabilities.canTranscribeAudio,
                        onAppendToNote = { result ->
                            onEvent(AddEditNoteEvent.AttachTranscript(result))
                            onEvent(AddEditNoteEvent.ClearImageQueryResult)
                        },
                    )

                    reminderAttachmentSection(
                        reminderTime = reminderTime,
                        onDelete = { onEvent(AddEditNoteEvent.CancelReminder) },
                        onClick = { onEvent(AddEditNoteEvent.CheckAlarmPermission) }
                    )

                    tagsSection(
                        tagEntities = tagEntities,
                        onDelete = { tagName -> onEvent(AddEditNoteEvent.DeleteLabel(tagName)) },
                    )

                    if (FeatureFlags.collaborationEnabled) {
                        collaboratorsSection(
                            collaborators = collaborators,
                            onCollaboratorClick = { onEvent(AddEditNoteEvent.OpenCollaborateSheet) }
                        )
                    }

                    item {
                        SuggestedTagsRow(
                            suggestions = suggestedTags,
                            isLoading = isSuggestionTagsLoading,
                            onTagAccepted = { tag -> onEvent(AddEditNoteEvent.InsertLabel(tag)) },
                            onDismiss = { onEvent(AddEditNoteEvent.ClearSuggestions) }
                        )
                    }
                }
            }
        }
    }
}
