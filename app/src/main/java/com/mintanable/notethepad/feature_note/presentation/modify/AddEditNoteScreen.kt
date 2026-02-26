package com.mintanable.notethepad.feature_note.presentation.modify

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.presentation.notes.AttachmentOptions
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType
import com.mintanable.notethepad.feature_note.presentation.notes.AudioSourceOptions
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.feature_note.presentation.notes.ImageSourceOptions
import com.mintanable.notethepad.feature_note.presentation.notes.MoreSettingsOptions
import com.mintanable.notethepad.feature_note.presentation.notes.ReminderOptions
import com.mintanable.notethepad.feature_note.presentation.notes.VideoSourceOptions
import com.mintanable.notethepad.feature_note.presentation.modify.components.AttachedImageItem
import com.mintanable.notethepad.feature_note.presentation.modify.components.AudioPlayerUI
import com.mintanable.notethepad.feature_note.presentation.modify.components.AudioRecorderUI
import com.mintanable.notethepad.feature_note.presentation.modify.components.NoteActionButtons
import com.mintanable.notethepad.feature_note.presentation.modify.components.BottomSheetContent
import com.mintanable.notethepad.feature_note.presentation.modify.components.ZoomedImageOverlay
import com.mintanable.notethepad.feature_note.presentation.notes.components.TransparentHintTextField
import com.mintanable.notethepad.feature_settings.presentation.components.PermissionRationaleDialog
import com.mintanable.notethepad.feature_settings.presentation.util.DeniedType
import com.mintanable.notethepad.feature_settings.presentation.util.NavigatationHelper
import com.mintanable.notethepad.feature_settings.presentation.util.PermissionRationaleType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditNoteScreen(
    navController: NavController,
    noteId: Int?,
    noteColor: Int,
    viewModel: AddEditNoteViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
){
    val context = LocalContext.current

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val titleState = viewModel.noteTitle.value
    val contentState = viewModel.noteContent.value
    val snackBarHostState = remember { SnackbarHostState() }
    val noteBackgroundAnimatable = remember{ Animatable(Color(if(noteColor!=-1) noteColor else viewModel.noteColor.value)) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var currentSheetType by rememberSaveable { mutableStateOf(BottomSheetType.NONE) }

    val attachedImageUris by viewModel.attachedImageUris.collectAsStateWithLifecycle()
    var zoomedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.onEvent(AddEditNoteEvent.AttachImage(it)) }
        }
    )

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    viewModel.onEvent(AddEditNoteEvent.AttachImage(uri))
                }
            }
        }
    )

    var cameraVideoUri by remember { mutableStateOf<Uri?>(null) }
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
        onResult = { success ->
            if (success) {
                cameraVideoUri?.let { uri ->
                    viewModel.onEvent(AddEditNoteEvent.AttachImage(uri))
                }
            }
        }
    )

    var settingsDeniedType by rememberSaveable { mutableStateOf<DeniedType?>(null) }
    var showCameraPermissionRationaleDialog by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    val checkAndRequestCameraPermission = { attachmentType: AttachmentType ->
        viewModel.checkCameraPermission(
            isGranted = cameraPermissionState.status.isGranted,
            shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
            attachmentType = attachmentType
        )

        if (!cameraPermissionState.status.isGranted &&
            !cameraPermissionState.status.shouldShowRationale) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val attachedAudioUris by viewModel.attachedAudioUris.collectAsStateWithLifecycle()
    var nowPlayingAudioUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    var showMicrophonePermissionRationaleDialog by rememberSaveable { mutableStateOf(false) }
    val microphonePermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val checkAndRequestMicrophonePermission = {
        viewModel.checkMicrophonePermission(
            isGranted = microphonePermissionState.status.isGranted,
            shouldShowRationale = microphonePermissionState.status.shouldShowRationale
        )
        // Physical trigger for first-time system popup
        if (!microphonePermissionState.status.isGranted &&
            !microphonePermissionState.status.shouldShowRationale) {
            microphonePermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(key1 = true){
        viewModel.eventFlow.collectLatest { event->
            when(event){
                is AddEditNoteViewModel.UiEvent.ShowSnackbar->{
                    snackBarHostState.showSnackbar( message = event.message)
                }
                is AddEditNoteViewModel.UiEvent.SaveNote->{
                    navController.navigateUp()
                }
                is AddEditNoteViewModel.UiEvent.ShowAudioRationale -> {
                    showMicrophonePermissionRationaleDialog = true
                }
                is AddEditNoteViewModel.UiEvent.OpenCameraSettings -> {
                    settingsDeniedType = DeniedType.CAMERA
                }
                is AddEditNoteViewModel.UiEvent.OpenMicrophoneSettings -> {
                    settingsDeniedType = DeniedType.MICROPHONE
                }
                is AddEditNoteViewModel.UiEvent.LaunchAudioRecorder -> {
                    currentSheetType = BottomSheetType.AUDIO_RECORDER
                }
                is AddEditNoteViewModel.UiEvent.LaunchCamera -> {
                    val uri = viewModel.generateTempUri(event.type)
                    if (event.type == AttachmentType.IMAGE) {
                        cameraImageUri = uri
                        cameraLauncher.launch(uri!!)
                    } else {
                        cameraVideoUri = uri
                        videoLauncher.launch(uri!!)
                    }
                }
                is AddEditNoteViewModel.UiEvent.ShowCameraRationale -> {
                    showCameraPermissionRationaleDialog = true
                }
            }
        }
    }

    with(sharedTransitionScope) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets.systemBars,
                floatingActionButton = {
                    NoteActionButtons(
                        modifier = Modifier,
                        onActionClick = { sheetType ->
                            currentSheetType = sheetType
                        },
                        onSaveClick = {
                            viewModel.onEvent(AddEditNoteEvent.SaveNote)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(noteBackgroundAnimatable.value)
            ) { paddingValue ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(noteBackgroundAnimatable.value)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = if (noteId == -1) "notescreens_fab" else "note-$noteId"
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValue)
                            .padding(horizontal = 16.dp)
                    ) {

                        item{
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                NoteColors.colors.forEach { color ->
                                    val colorInt = color.toArgb()
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .shadow(15.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = 3.dp,
                                                color = if (viewModel.noteColor.value == colorInt) {
                                                    Color.Black
                                                } else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                scope.launch {
                                                    noteBackgroundAnimatable.animateTo(
                                                        targetValue = Color(colorInt),
                                                        animationSpec = tween(
                                                            durationMillis = 500
                                                        )
                                                    )
                                                }
                                                viewModel.onEvent(AddEditNoteEvent.ChangeColor(colorInt))
                                            }
                                    )
                                }
                            }
                        }

                        if (attachedImageUris.isNotEmpty()) {
                            item{
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    items(
                                        items = attachedImageUris,
                                        key = { uri -> uri.toString() },
                                    ) { uri ->
                                        AttachedImageItem(
                                            uri = uri,
                                            onDelete = { deletedUri ->
                                                viewModel.onEvent(
                                                    AddEditNoteEvent.RemoveImage( deletedUri )
                                                )
                                            },
                                            onClick = { uri ->
                                                zoomedImageUri = uri
                                            },
                                            modifier = Modifier.sharedBounds(
                                                sharedContentState = rememberSharedContentState(key = "image-${uri}"),
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        )
                                    }
                                }
                            }

                        }

                        item{
                            Spacer(modifier = Modifier.height(16.dp))
                            TransparentHintTextField(
                                text = titleState.text,
                                hint = titleState.hint,
                                onValueChange = {
                                    viewModel.onEvent(AddEditNoteEvent.EnteredTitle(it))
                                },
                                onFocusChange = {
                                    viewModel.onEvent(AddEditNoteEvent.ChangeTitleFocus(it))
                                },
                                isHintVisible = titleState.isHintVisible,
                                isSingleLine = true,
                                textStyle = MaterialTheme.typography.headlineLarge,
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
                            TransparentHintTextField(
                                text = contentState.text,
                                hint = contentState.hint,
                                onValueChange = {
                                    viewModel.onEvent(AddEditNoteEvent.EnteredContent(it))
                                },
                                onFocusChange = {
                                    viewModel.onEvent(AddEditNoteEvent.ChangeContentFocus(it))
                                },
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

                        if (attachedAudioUris.isNotEmpty()) {
                            item{
                                Spacer(modifier = Modifier.height(16.dp))
                                Column {
                                    attachedAudioUris.forEach { audioUri ->
                                        AudioPlayerUI(
                                            uri = audioUri,
                                            nowPlaying = audioUri == nowPlayingAudioUri,
                                            onDelete = { deletedUri ->
                                                viewModel.onEvent(AddEditNoteEvent.RemoveAudio(deletedUri))
                                            },
                                            onPlayPause = { uri ->
                                                nowPlayingAudioUri = uri
                                            }
                                        )
                                    }
                                }
                            }


                        }
                    }
                }
            }

            val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navigationBarHeight + 8.dp)
                    .padding(start = 16.dp, end = 100.dp)
            )

            if (zoomedImageUri != null) {
                ZoomedImageOverlay(
                    uri = zoomedImageUri!!,
                    onClick = { zoomedImageUri = null },
                    transitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }

            if (showCameraPermissionRationaleDialog) {
                PermissionRationaleDialog(
                    permissionRationaleType = PermissionRationaleType.CAMERA,
                    onConfirmClicked = {
                        showCameraPermissionRationaleDialog = false
                        cameraPermissionState.launchPermissionRequest()
                    },
                    onDismissRequest = { showCameraPermissionRationaleDialog = false }
                )
            }

            if (showMicrophonePermissionRationaleDialog) {
                PermissionRationaleDialog(
                    permissionRationaleType = PermissionRationaleType.MICROPHONE,
                    onConfirmClicked = {
                        showMicrophonePermissionRationaleDialog = false
                        microphonePermissionState.launchPermissionRequest()
                    },
                    onDismissRequest = { showMicrophonePermissionRationaleDialog = false }
                )
            }

            settingsDeniedType?.let { type ->
                PermissionRationaleDialog(
                    permissionRationaleType =
                        if (type == DeniedType.CAMERA)
                            PermissionRationaleType.CAMERA_DENIED
                        else
                            PermissionRationaleType.MICROPHONE_DENIED,
                    onConfirmClicked = {
                        settingsDeniedType = null // Resets both cases at once
                        NavigatationHelper.openAppSettings(context)
                    },
                    onDismissRequest = {
                        settingsDeniedType = null
                    }
                )
            }
        }
    }

    val sheetItems = remember(currentSheetType) {
        when (currentSheetType) {
            BottomSheetType.ATTACH -> AttachmentOptions.entries
            BottomSheetType.REMINDER -> ReminderOptions.entries
            BottomSheetType.MORE_SETTINGS -> MoreSettingsOptions.entries
            BottomSheetType.IMAGE_SOURCES -> ImageSourceOptions.entries
            BottomSheetType.VIDEO_SOURCES -> VideoSourceOptions.entries
            BottomSheetType.AUDIO_SOURCES -> AudioSourceOptions.entries
            else -> emptyList()
        }
    }

    if (currentSheetType != BottomSheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest =
                {
                    currentSheetType = BottomSheetType.NONE
                },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            if(currentSheetType == BottomSheetType.AUDIO_RECORDER){
                AudioRecorderUI(
                    isRecording = isRecording,
                    onStartRecordingClicked = {
                        viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)
                    },
                    onStopRecordingClicked = {
                        viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)
                    }
                )
            } else if (currentSheetType != BottomSheetType.NONE) {
                BottomSheetContent(
                    items = sheetItems,
                    optionSelected = { additionalOption ->
                        when(additionalOption){
                            AttachmentOptions.IMAGE -> {
                                currentSheetType = BottomSheetType.IMAGE_SOURCES
                            }

                            AttachmentOptions.VIDEO -> {
                                currentSheetType = BottomSheetType.VIDEO_SOURCES
                            }

                            AttachmentOptions.AUDIO -> {
                                currentSheetType = BottomSheetType.AUDIO_SOURCES
                            }

                            ImageSourceOptions.PHOTO_GALLERY -> {
                                currentSheetType = BottomSheetType.NONE
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            ImageSourceOptions.PHOTO_CAMERA -> {
                                currentSheetType = BottomSheetType.NONE
                                checkAndRequestCameraPermission(AttachmentType.IMAGE)
                            }

                            VideoSourceOptions.VIDEO_CAMERA -> {
                                currentSheetType = BottomSheetType.NONE
                                checkAndRequestCameraPermission(AttachmentType.VIDEO)
                            }

                            VideoSourceOptions.VIDEO_GALLERY -> {
                                currentSheetType = BottomSheetType.NONE
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }

                            AudioSourceOptions.AUDIO_RECORDER -> {
                                currentSheetType = BottomSheetType.NONE
                                checkAndRequestMicrophonePermission()
                            }

                            else -> {}
                        }
                    }
                )
            }
        }
    }

    if (isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {}, // Blocks touch events
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Saving Note...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}