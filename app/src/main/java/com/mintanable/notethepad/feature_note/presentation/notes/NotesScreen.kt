package com.mintanable.notethepad.feature_note.presentation.notes

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mintanable.notethepad.feature_note.presentation.notes.components.TopSearchBar
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel
import com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.NavigationDrawerViewModel
import com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.components.AppDrawer
import com.mintanable.notethepad.ui.util.Screen
import kotlinx.coroutines.launch
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.presentation.notes.components.EvenHandler
import com.mintanable.notethepad.feature_note.presentation.notes.components.OrderSection
import com.mintanable.notethepad.feature_note.presentation.notes.components.StaggeredNotesList
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen (
    navController: NavController,
    notesViewModel: NotesViewModel = hiltViewModel(),
    navigationDrawerViewModel: NavigationDrawerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onLogOut: suspend () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
){
    val state by notesViewModel.state.collectAsStateWithLifecycle()
    val navigationDrawerState by navigationDrawerViewModel.navigationDrawerState.collectAsStateWithLifecycle()
    val searchQuery = notesViewModel.searchInputText.collectAsState().value
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isGridView by notesViewModel.isGridViewEnabled.collectAsStateWithLifecycle()
    val isOrderSectionVisible by notesViewModel.isOrderSectionVisible.collectAsStateWithLifecycle()
    val currentOrder by notesViewModel.noteOrder.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(user) {
        navigationDrawerViewModel.onLoggedIn(user != null)
    }

    val context = LocalContext.current
    LaunchedEffect(state.notes, context){
        notesViewModel.updateNoteWidget(context)
    }
    val snackBarHostState = remember { SnackbarHostState() }
    EvenHandler(snackBarHostState = snackBarHostState)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                user = user,
                items = navigationDrawerState.items,
                selectedItemIndex =  selectedItemIndex,
                onItemSelected = { index, navigationItem ->
                    selectedItemIndex = index
                    scope.launch {
                        drawerState.close()
                    }

                    if(navigationItem.route == "logout"){
                        scope.launch {
                            authViewModel.signOut()
                            onLogOut()
                        }
                    } else {
                        navController.navigate(navigationItem.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ){

        with(sharedTransitionScope){
            val onNoteClick = remember(navController) {
                { note: DetailedNote ->
                    navController.navigate(
                        Screen.AddEditNoteScreen.route + "?noteId=${note.id}&noteColor=${note.color}"
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
                                isGridView = isGridView,
                                onToogleGridView = {
                                    notesViewModel.toggleGridView(it)
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
                                    contentDescription = "Menu"
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(Screen.AddEditNoteScreen.route)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = sharedTransitionScope.rememberSharedContentState(key = "notescreens_fab"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                exit = fadeOut(tween(durationMillis = 100)),
                                boundsTransform = { _, _ ->
                                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow )
                                                  },
                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                )
                            .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 2f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add note")
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

                    if(state.notes.isNotEmpty()) {
                        StaggeredNotesList(
                            notes = state.notes,
                            isGridView = isGridView,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onNoteClicked = onNoteClick,
                            onDeleteClicked = { note ->
                                notesViewModel.onEvent(
                                    NotesEvent.DeleteNote(
                                        note
                                    )
                                )
                            },
                            onPinClicked = { note -> notesViewModel.onEvent(NotesEvent.PinNote(note)) }
                        )
                    }else{
                        val isDark = settings.themeMode == ThemeMode.DARK
                        val resource =
                            if(isDark){
                                if(searchQuery.isNotEmpty())
                                    R.drawable.search_female_dark
                                else
                                    R.drawable.empty_male_dark
                            } else {
                                if(searchQuery.isNotEmpty())
                                    R.drawable.search_female_pastel
                                else
                                    R.drawable.empty_male_pastel
                            }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(resource),
                                contentDescription = "empty list"
                            )
                        }
                    }
                }
            }
        }
    }
}