package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel
import com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.NavigationDrawerViewModel
import com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.components.AppDrawer
import com.mintanable.notethepad.ui.util.Screen
import com.mintanable.notethepad.feature_note.presentation.notes.NotesEvent
import com.mintanable.notethepad.feature_note.presentation.notes.NotesViewModel
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import kotlinx.coroutines.launch
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import com.mintanable.notethepad.feature_note.domain.model.Note

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NotesScreen (
    navController: NavController,
    notesViewModel: NotesViewModel = hiltViewModel(),
    navigationDrawerViewModel: NavigationDrawerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogOut: suspend () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
){
    val state by notesViewModel.state
    val navigationDrawerState by navigationDrawerViewModel.navigationDrawerState.collectAsStateWithLifecycle()
    val searchQuery = notesViewModel.searchInputText.collectAsState().value
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isGridView by notesViewModel.isGridViewEnabled.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(user) {
        navigationDrawerViewModel.onLoggedIn(user != null)
    }

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
                { note: Note ->
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
//                            .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
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
                        visible = state.isOrderSectionVisible,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()

                    ) {
                        OrderSection(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            noteOrder = state.noteOrder,
                            onOrderChange = {
                                notesViewModel.onEvent(NotesEvent.Order(it))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        items(
                            state.notes,
                            key = { note -> note.id ?: -1 },
                            contentType = { "note_item" }
                        ){ note->
                            NoteItem(
                                note = note,
                                modifier = Modifier
//                                    .animateItem(
//                                        fadeInSpec = tween(300),
//                                        fadeOutSpec = tween(300),
//                                        placementSpec = spring(
//                                            dampingRatio = Spring.DampingRatioLowBouncy,
//                                            stiffness = Spring.StiffnessMedium
//                                        )
//                                    )
                                    .fillMaxWidth()
                                    .sharedBounds(
                                        sharedContentState = sharedTransitionScope.rememberSharedContentState(key = "note-${note.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ ->
                                            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow )
                                        },
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                    )
//                                    .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 0f)
                                    .clickable { onNoteClick(note) },
                                onDeleteClick = {
                                    notesViewModel.onEvent(NotesEvent.DeleteNote(note))
                                    scope.launch {
                                        val result = snackBarHostState.showSnackbar(
                                            message = "Note deleted",
                                            actionLabel = "Undo"
                                        )
                                        if(result == SnackbarResult.ActionPerformed){
                                            notesViewModel.onEvent(NotesEvent.RestoreNote)
                                        }
                                    }
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun test(){
    NoteThePadTheme {
        Text(
            "NoteThePad",
            modifier = Modifier
                .padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )
    }
}