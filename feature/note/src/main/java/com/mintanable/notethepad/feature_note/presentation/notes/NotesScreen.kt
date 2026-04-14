package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mintanable.notethepad.core.common.AppVersionProvider
import com.mintanable.notethepad.core.common.NotesFilterType
import com.mintanable.notethepad.core.common.Screen
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.database.db.entity.DrawerItem
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.core.model.settings.User
import com.mintanable.notethepad.feature_ai.BuildConfig
import com.mintanable.notethepad.feature_note.presentation.navigationdrawer.NavigationDrawerViewModel
import com.mintanable.notethepad.feature_note.presentation.navigationdrawer.components.AppDrawer
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.assistant.AiAssistantDialog
import com.mintanable.notethepad.feature_note.presentation.assistant.AiAssistantViewModel
import com.mintanable.notethepad.feature_note.presentation.modify.components.HomeActionButtons
import com.mintanable.notethepad.feature_note.presentation.notes.components.EditTextDialog
import com.mintanable.notethepad.feature_note.presentation.notes.components.EvenHandler
import com.mintanable.notethepad.feature_note.presentation.notes.components.OrderSection
import com.mintanable.notethepad.feature_note.presentation.notes.components.StaggeredNotesList
import com.mintanable.notethepad.feature_note.presentation.notes.components.TopSearchBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
    notesViewModel: NotesViewModel = hiltViewModel(),
    navigationDrawerViewModel: NavigationDrawerViewModel = hiltViewModel(),
    aiAssistantViewModel: AiAssistantViewModel = hiltViewModel(),
    user: User? = null,
    isDarkTheme: Boolean,
    onLogOut: suspend () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    appVersionProvider: AppVersionProvider,
    onPinWidget: (NoteEntity) -> Unit = {}
) {
    val state by notesViewModel.state.collectAsStateWithLifecycle()
    val navigationDrawerState by navigationDrawerViewModel.navigationDrawerState.collectAsStateWithLifecycle()
    val searchQuery by notesViewModel.searchInputText.collectAsStateWithLifecycle()
    val noteShape by notesViewModel.noteShape.collectAsStateWithLifecycle()
    val isOrderSectionVisible by notesViewModel.isOrderSectionVisible.collectAsStateWithLifecycle()
     val showLabelDialog by notesViewModel.showLabelDialog.collectAsStateWithLifecycle()
    val filterState by notesViewModel.filterState.collectAsStateWithLifecycle()
    val isFiltered = filterState.filter != NotesFilterType.ALL.filter
    val currentOrder = filterState.order
    val existingTags by navigationDrawerViewModel.existingTags.collectAsStateWithLifecycle()
    val supaSyncEnabled by notesViewModel.supaSyncEnabled.collectAsStateWithLifecycle()
    val isSupaSyncing by notesViewModel.isSupaSyncing.collectAsStateWithLifecycle()
    val aiAssistantState by aiAssistantViewModel.state.collectAsStateWithLifecycle()
    val aiAssistantEnabled by notesViewModel.aiAssistantEnabled.collectAsStateWithLifecycle()
    val isAiAssistantSupported = BuildConfig.ENABLE_AI_ASSISTANCE && aiAssistantEnabled

    val scope = rememberCoroutineScope()
    var showLogoutConfirmation by rememberSaveable { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(user) {
        navigationDrawerViewModel.onLoggedIn(user != null)
    }

    BackHandler(enabled = isFiltered) {
        notesViewModel.updateFilter(NotesFilterType.ALL.filter)
    }

    val context = LocalContext.current
    LaunchedEffect(state.notes, context) {
        notesViewModel.refreshWidget()
    }
    val snackBarHostState = remember { SnackbarHostState() }
    var showLogoAnimation by remember { mutableStateOf(NotesViewModel.shouldShowLogoAnimation) }
    EvenHandler(snackBarHostState = snackBarHostState, onPinWidget = onPinWidget)

    if (showLabelDialog) {
        EditTextDialog(
            onDismiss = { notesViewModel.onEvent(NotesEvent.DismissLabelDialog) },
            onConfirm = { notesViewModel.onEvent(NotesEvent.AddLabel(it)) },
            tagEntities = existingTags
        )
    }

    if (isAiAssistantSupported) {
        AiAssistantDialog(
            state = aiAssistantState,
            onPromptChange = aiAssistantViewModel::onPromptChange,
            onSubmit = aiAssistantViewModel::submit,
            onDismiss = aiAssistantViewModel::dismiss,
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                user = user,
                items = navigationDrawerState.items,
                selectedItemIndex = selectedItemIndex,
                onItemSelected = { index, item ->
                    selectedItemIndex = index
                    scope.launch { drawerState.close() }
                    when (item) {
                        is DrawerItem.LabelDrawerItem -> {
                            notesViewModel.updateFilter(
                                NotesFilterType.TAGS.filter,
                                item.tagEntity.tagId,
                                item.tagEntity.tagName
                            )
                        }

                        is DrawerItem.NavigationDrawerItem -> {
                            if (item.title == "Notes") notesViewModel.updateFilter(NotesFilterType.ALL.filter)
                            else if (item.title == "Reminders") notesViewModel.updateFilter(
                                NotesFilterType.REMINDERS.filter
                            )
                            else if (item.route == Screen.LogOut.route) {
                                showLogoutConfirmation = true
                            } else {
                                navController.navigate(item.route)
                            }
                        }

                        is DrawerItem.AddLabelDrawerItem -> {
                            notesViewModel.onEvent(NotesEvent.ShowLabelDialog)
                        }

                        else -> Unit
                    }
                },
                onTagDeleted = { tag -> notesViewModel.onEvent(NotesEvent.DeleteLabel(tag)) },
                onTagEdited = { tag -> notesViewModel.onEvent(NotesEvent.EditLabel(tag)) },
                appVersionProvider = appVersionProvider,
                isSyncing = navigationDrawerState.isSyncing
            )
        }
    ) {
        with(sharedTransitionScope) {
            val onNoteClick = remember(navController) {
                { note: DetailedNote ->
                    navController.navigate(
                        Screen.AddEditNoteScreen.passArgs(noteId = note.id)
                    )
                }
            }

            Scaffold(
                contentWindowInsets = WindowInsets.systemBars,
                topBar = {
                    TopAppBar(
                        title = {
                            TopSearchBar(
                                searchQuery,
                                showLogoAnimation = showLogoAnimation,
                                onLogoAnimationComplete = {
                                    NotesViewModel.markAnimationShown()
                                    showLogoAnimation = false
                                },
                                onValueChange = {
                                    notesViewModel.onEvent(NotesEvent.SearchBarValueChange(it))
                                },
                                onFocusChanged = {
                                },
                                onClearClicked = {
                                    notesViewModel.onEvent(NotesEvent.SearchBarValueChange(""))
                                },
                                onExpandClicked = {
                                    notesViewModel.onEvent(NotesEvent.ToggleOrderSection)
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.apply {
                                        if (isClosed) open() else close()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.content_description_menu)
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    HomeActionButtons(
                        modifier = Modifier,
                        isAiAssitantSupported = isAiAssistantSupported,
                        onAssitantClicked = { aiAssistantViewModel.show() }
                    ) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(Screen.AddEditNoteScreen.passArgs())
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .sharedBounds(
                                    sharedContentState = sharedTransitionScope.rememberSharedContentState(key = "notescreens_fab"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    exit = fadeOut(tween(durationMillis = 100)),
                                    boundsTransform = { _, _ ->
                                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                                    },
                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                )
                                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 2f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_note))
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) { paddingValue ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValue)
                        .padding(horizontal = 16.dp)
                ) {
                    AnimatedVisibility(
                        visible = isOrderSectionVisible,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        OrderSection(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            noteOrder = currentOrder,
                            onOrderChange = { notesViewModel.onEvent(NotesEvent.Order(it)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = !showLogoAnimation,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(0))
                    ) {
                        if (state.notes.isNotEmpty()) {
                            StaggeredNotesList(
                                notes = state.notes,
                                noteShape = noteShape,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onNoteClicked = onNoteClick,
                                onDeleteClicked = { note ->
                                    notesViewModel.onEvent(
                                        NotesEvent.DeleteNote(note)
                                    )
                                },
                                onPinClicked = { note ->
                                    notesViewModel.onEvent(
                                        NotesEvent.PinNote(
                                            note
                                        )
                                    )
                                },
                                isDarkTheme = isDarkTheme,
                                isSyncEnabled = supaSyncEnabled,
                                isSyncing = isSupaSyncing,
                                onRefresh = { notesViewModel.onEvent(NotesEvent.RefreshCloudNotes) }
                            )
                        } else {
                            val resource =
                                if (searchQuery.isNotEmpty())
                                    R.drawable.search_female
                                else
                                    R.drawable.empty_male
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(resource),
                                    contentDescription = stringResource(R.string.content_description_empty_list)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text(stringResource(R.string.dialog_logout_title)) },
            text = { Text(stringResource(R.string.dialog_logout_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmation = false
                        scope.launch {
                            onLogOut()
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_logout))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmation = false }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
