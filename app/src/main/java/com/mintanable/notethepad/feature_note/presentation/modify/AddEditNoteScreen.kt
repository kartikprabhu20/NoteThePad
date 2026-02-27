package com.mintanable.notethepad.feature_note.presentation.modify

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.mintanable.notethepad.ui.util.Screen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditNoteScreen(
    navController: NavController,
    noteId: Long?,
    noteColor: Int,
    viewModel: AddEditNoteViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
){
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }
    val noteBackgroundAnimatable = remember{ Animatable(Color(if(noteColor!=-1) noteColor else uiState.noteColor)) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

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

   val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
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

    val hideSheetAndNavigate = { action: () -> Unit ->
        scope.launch {
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
            action()
        }
    }

    LaunchedEffect(key1 = true){
        viewModel.eventFlow.collectLatest { event->
            when(event){
                is AddEditNoteViewModel.UiEvent.ShowSnackbar->{
                    snackBarHostState.showSnackbar( message = event.message)
                }
                is AddEditNoteViewModel.UiEvent.SaveNote->{
                    scope.launch {
                        sheetState.hide()
                        viewModel.onEvent(AddEditNoteEvent.StopMedia)
                        navController.navigateUp()
                    }
                }
                is AddEditNoteViewModel.UiEvent.MakeCopy -> {
                    hideSheetAndNavigate {
                        viewModel.onEvent(AddEditNoteEvent.StopMedia)
                        navController.navigate(
                            Screen.AddEditNoteScreen.route +
                                    "?noteId=${event.newNoteId}&noteColor=${uiState.noteColor}"
                        ) {
                            popUpTo(Screen.AddEditNoteScreen.route + "?noteId={noteId}&noteColor={noteColor}") {
                                inclusive = true
                            }
                        }
                    }
                }
                is AddEditNoteViewModel.UiEvent.DeleteNote -> {
                    hideSheetAndNavigate {
                        viewModel.onEvent(AddEditNoteEvent.StopMedia)
                        navController.navigateUp()
                    }
                }
                is AddEditNoteViewModel.UiEvent.LaunchAudioRecorder -> {
                    viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.AUDIO_RECORDER))
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
                else -> {}
            }
        }
    }

    BackHandler {
        viewModel.onEvent(AddEditNoteEvent.StopMedia)
        if(uiState.zoomedImageUri == null){
            navController.navigateUp()
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
                            viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(sheetType))
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
                                key = if (noteId == -1L) "notescreens_fab" else "note-$noteId"
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
                                                color = if (uiState.noteColor == colorInt) {
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

                        if (uiState.attachedImages.isNotEmpty()) {
                            item{
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    items(
                                        items = uiState.attachedImages,
                                        key = { uri -> uri.toString() },
                                    ) { uri ->
                                        AttachedImageItem(
                                            uri = uri,
                                            onDelete = { deletedUri ->
                                                viewModel.onEvent(
                                                    AddEditNoteEvent.RemoveImage( deletedUri )
                                                )
                                            },
                                            onClick = {
                                                viewModel.onEvent(
                                                    AddEditNoteEvent.ToggleZoom( it )
                                                )
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
                                text = uiState.titleState.text,
                                hint = uiState.titleState.hint,
                                onValueChange = {
                                    viewModel.onEvent(AddEditNoteEvent.EnteredTitle(it))
                                },
                                onFocusChange = {
                                    viewModel.onEvent(AddEditNoteEvent.ChangeTitleFocus(it))
                                },
                                isHintVisible = uiState.titleState.isHintVisible,
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
                            TransparentHintTextField(
                                text = uiState.contentState.text,
                                hint = uiState.contentState.hint,
                                onValueChange = {
                                    viewModel.onEvent(AddEditNoteEvent.EnteredContent(it))
                                },
                                onFocusChange = {
                                    viewModel.onEvent(AddEditNoteEvent.ChangeContentFocus(it))
                                },
                                isHintVisible = uiState.contentState.isHintVisible,
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

                        if (uiState.attachedAudios.isNotEmpty()) {
                            item{
                                Spacer(modifier = Modifier.height(16.dp))
                                Column {
                                    uiState.attachedAudios.forEach { audioUri ->
                                        AudioPlayerUI(
                                            attachment = audioUri,
                                            playbackState = uiState.mediaState,
                                            onDelete = { deletedUri ->
                                                viewModel.onEvent(AddEditNoteEvent.RemoveAudio(deletedUri))
                                            },
                                            onPlayPause = { uri ->
                                                viewModel.onEvent(AddEditNoteEvent.UpdateNowPlaying(uri))
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

            if (uiState.zoomedImageUri != null) {
                ZoomedImageOverlay(
                    uri = uiState.zoomedImageUri!!,
                    playerEngine = viewModel.videoPlayerEngine,
                    onClick = { viewModel.onEvent(AddEditNoteEvent.StopMedia) },
                    transitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }

            if (uiState.showCameraRationale) {
                PermissionRationaleDialog(
                    permissionRationaleType = PermissionRationaleType.CAMERA,
                    onConfirmClicked = {
                        viewModel.onEvent(AddEditNoteEvent.DismissDialogs)
                        cameraPermissionState.launchPermissionRequest()
                    },
                    onDismissRequest = { viewModel.onEvent(AddEditNoteEvent.DismissDialogs) }
                )
            }

            if (uiState.showMicrophoneRationale) {
                PermissionRationaleDialog(
                    permissionRationaleType = PermissionRationaleType.MICROPHONE,
                    onConfirmClicked = {
                        viewModel.onEvent(AddEditNoteEvent.DismissDialogs)
                        microphonePermissionState.launchPermissionRequest()
                    },
                    onDismissRequest = { viewModel.onEvent(AddEditNoteEvent.DismissDialogs) }
                )
            }

            uiState.settingsDeniedType?.let { type ->
                PermissionRationaleDialog(
                    permissionRationaleType = if (type == DeniedType.CAMERA)
                        PermissionRationaleType.CAMERA_DENIED else PermissionRationaleType.MICROPHONE_DENIED,
                    onConfirmClicked = {
                        viewModel.onEvent(AddEditNoteEvent.DismissDialogs)
                        NavigatationHelper.openAppSettings(context)
                    },
                    onDismissRequest = { viewModel.onEvent(AddEditNoteEvent.DismissDialogs) }
                )
            }
        }
    }

    val sheetItems = remember(uiState.currentSheetType) {
        when (uiState.currentSheetType) {
            BottomSheetType.ATTACH -> AttachmentOptions.entries
            BottomSheetType.REMINDER -> ReminderOptions.entries
            BottomSheetType.MORE_SETTINGS -> MoreSettingsOptions.entries
            BottomSheetType.IMAGE_SOURCES -> ImageSourceOptions.entries
            BottomSheetType.VIDEO_SOURCES -> VideoSourceOptions.entries
            BottomSheetType.AUDIO_SOURCES -> AudioSourceOptions.entries
            else -> emptyList()
        }
    }

    if (uiState.currentSheetType != BottomSheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest =
                {
                    if (!uiState.isRecording) {
                        viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                    }
                },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            if(uiState.currentSheetType == BottomSheetType.AUDIO_RECORDER){
                AudioRecorderUI(
                    isRecording = uiState.isRecording,
                    onStartRecordingClicked = {
                        viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)
                    },
                    onStopRecordingClicked = {
                        viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)
                    }
                )
            } else {
                BottomSheetContent(
                    items = sheetItems,
                    optionSelected = { additionalOption ->
                        when(additionalOption){
                            AttachmentOptions.IMAGE -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.IMAGE_SOURCES))
                            }

                            AttachmentOptions.VIDEO -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.VIDEO_SOURCES))

                            }

                            AttachmentOptions.AUDIO -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.AUDIO_SOURCES))
                            }

                            ImageSourceOptions.PHOTO_GALLERY -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            ImageSourceOptions.PHOTO_CAMERA -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                                checkAndRequestCameraPermission(AttachmentType.IMAGE)
                            }

                            VideoSourceOptions.VIDEO_CAMERA -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                                checkAndRequestCameraPermission(AttachmentType.VIDEO)
                            }

                            VideoSourceOptions.VIDEO_GALLERY -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }

                            AudioSourceOptions.AUDIO_RECORDER -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                                checkAndRequestMicrophonePermission()
                            }

                            MoreSettingsOptions.COPY -> {
                                viewModel.onEvent(AddEditNoteEvent.MakeCopy)
                            }

                            MoreSettingsOptions.DELETE -> {
                                viewModel.onEvent(AddEditNoteEvent.DeleteNote)
                            }

                            MoreSettingsOptions.SHARE -> {

                            }

                            else -> {}
                        }
                    }
                )
            }
        }
    }

    if (uiState.isSaving) {
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