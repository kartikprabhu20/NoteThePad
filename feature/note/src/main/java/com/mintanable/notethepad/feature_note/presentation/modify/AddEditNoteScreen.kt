package com.mintanable.notethepad.feature_note.presentation.modify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mintanable.notethepad.core.common.Screen
import com.mintanable.notethepad.core.model.note.AttachmentType
import com.mintanable.notethepad.feature_note.presentation.modify.components.AudioRecorderUI
import com.mintanable.notethepad.feature_note.presentation.modify.components.BottomSheetContent
import com.mintanable.notethepad.feature_note.presentation.modify.components.DateAndTimePicker
import com.mintanable.notethepad.feature_note.presentation.modify.components.SavingOverlay
import com.mintanable.notethepad.feature_note.presentation.modify.components.ZoomedImageOverlay
import com.mintanable.notethepad.feature_note.presentation.modify.components.sections.NoteEditorContent
import com.mintanable.notethepad.feature_note.presentation.notes.AttachmentOptions
import com.mintanable.notethepad.feature_note.presentation.notes.AudioSourceOptions
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.feature_note.presentation.notes.ImageSourceOptions
import com.mintanable.notethepad.feature_note.presentation.notes.MoreSettingsOptions
import com.mintanable.notethepad.feature_note.presentation.notes.ReminderOptions
import com.mintanable.notethepad.feature_note.presentation.notes.VideoSourceOptions
import com.mintanable.notethepad.feature_settings.presentation.components.EditTextDialog
import com.mintanable.notethepad.feature_settings.presentation.components.PermissionRationaleDialog
import com.mintanable.notethepad.feature_settings.presentation.util.DeniedType
import com.mintanable.notethepad.feature_settings.presentation.util.NavigatationHelper
import com.mintanable.notethepad.feature_settings.presentation.util.PermissionRationaleType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditNoteScreen(
    navController: NavController,
    noteId: Long,
    viewModel: AddEditNoteViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    onPinWidget: (Long) -> Unit = {}
){
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val existingTags by viewModel.existingTags.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { sheetValue ->
            if (uiState.isRecording) {
                sheetValue != SheetValue.Hidden
            } else {
                true
            }
        }
    )

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

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
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

    val microphonePermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
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

    val focusManager = LocalFocusManager.current
    LaunchedEffect(key1 = true){
        viewModel.eventFlow.collect { event ->
            when(event){
                is AddEditNoteViewModel.UiEvent.ShowSnackbar->{
                    snackBarHostState.showSnackbar( message = event.message)
                }
                is AddEditNoteViewModel.UiEvent.SaveNote->{
                    focusManager.clearFocus()
                    withContext(NonCancellable) {
                        if (sheetState.isVisible) {
                            sheetState.hide()
                        }
                        navController.navigateUp()
                        viewModel.onEvent(AddEditNoteEvent.StopMedia)
                    }
                }

                is AddEditNoteViewModel.UiEvent.MakeCopy -> {
                    hideSheetAndNavigate {
                        viewModel.onEvent(AddEditNoteEvent.StopMedia)
                        navController.navigate(
                            Screen.AddEditNoteScreen.passArgs(noteId=event.newNoteId)
                        ) {
                            popUpTo(Screen.AddEditNoteScreen.route + "?noteId={noteId}") {
                                inclusive = true
                            }
                        }
                    }
                }
                is AddEditNoteViewModel.UiEvent.DeleteNote -> {
                    withContext(NonCancellable) {
                        hideSheetAndNavigate {
                            viewModel.onEvent(AddEditNoteEvent.StopMedia)
                            navController.navigateUp()
                        }
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

                is AddEditNoteViewModel.UiEvent.RequestWidgetPin -> {
                    event.noteId?.let { onPinWidget(it) }
                }
            }
        }
    }

    BackHandler {
        viewModel.onEvent(AddEditNoteEvent.StopMedia)
        if(uiState.zoomedImageUri == null){
            navController.navigateUp()
        }
        when {
            uiState.zoomedImageUri != null -> {
                viewModel.onEvent(AddEditNoteEvent.StopMedia)
            }
            uiState.currentSheetType != BottomSheetType.NONE && uiState.isRecording -> {
                viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)
            }
            uiState.currentSheetType != BottomSheetType.NONE -> {
                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
            }
            else -> {
                viewModel.onEvent(AddEditNoteEvent.StopMedia)
                navController.navigateUp()
            }
        }
    }

    val onEvent = remember { { event: AddEditNoteEvent -> viewModel.onEvent(event) } }

    Box(modifier = Modifier.fillMaxSize()) {
            NoteEditorContent(
                noteId = noteId,
                noteColor = uiState.noteColor,
                attachedImages = uiState.attachedImages,
                titleState = uiState.titleState,
                contentState = uiState.contentState,
                isCheckboxListAvailable = uiState.isCheckboxListAvailable,
                checkListItems = uiState.checkListItems,
                attachedAudios = uiState.attachedAudios,
                mediaState = uiState.mediaState,
                reminderTime = uiState.reminderTime,
                tags = uiState.tags,
                suggestedTags = uiState.suggestedTags,
                isSuggestionTagsLoading = uiState.isTagSuggestionLoading,
                onEvent = onEvent,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )

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

        if(uiState.showAlarmPermissionRationale){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }

        if(uiState.showDataAndTimePicker){
            DateAndTimePicker(
                onDismiss = { viewModel.onEvent(AddEditNoteEvent.DismissReminder) },
                onConfirm = { selectedTimestamp ->
                    viewModel.onEvent(AddEditNoteEvent.SetReminder(selectedTimestamp))
                }
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

        if (uiState.showAddNewTagDialog){
            EditTextDialog(
                onDismiss = { viewModel.onEvent(AddEditNoteEvent.DismissDialogs)} ,
                onConfirm = {tagName -> viewModel.onEvent(AddEditNoteEvent.InsertLabel(tagName))},
                tags = existingTags
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
                    if (uiState.isRecording) {
                        viewModel.onEvent(AddEditNoteEvent.ToggleAudioRecording)
                    }
                    viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                },
            sheetState = sheetState,
            dragHandle = if (uiState.isRecording) null else { { BottomSheetDefaults.DragHandle() } }        ) {
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

                            MoreSettingsOptions.PIN -> {
                                viewModel.onEvent(AddEditNoteEvent.PinNote)
                            }

                            MoreSettingsOptions.LABEL -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                                viewModel.onEvent(AddEditNoteEvent.ShowLabelDialog)
                            }

//                            MoreSettingsOptions.SHARE -> {
//
//                            }

                            ReminderOptions.DATE_AND_TIME -> {
                                viewModel.onEvent(AddEditNoteEvent.UpdateSheetType(BottomSheetType.NONE))
                                viewModel.checkExactAlarmPermission()
                            }
                            else -> {}
                        }
                    }
                )
            }
        }
    }

    SavingOverlay(uiState.isSaving)
}

