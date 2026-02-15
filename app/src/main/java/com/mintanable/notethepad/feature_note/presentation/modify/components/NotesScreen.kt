package com.mintanable.notethepad.feature_note.presentation.modify.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen (
    navController: NavController,
    notesViewModel: NotesViewModel = hiltViewModel(),
    navigationDrawerViewModel: NavigationDrawerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogOut: suspend () -> Unit
){
    val state = notesViewModel.state.value
    val navigationDrawerState by navigationDrawerViewModel.navigationDrawerState.collectAsStateWithLifecycle()
    val searchQuery = notesViewModel.searchInputText.collectAsState().value
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    Log.i("kotest", "user: $user")

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

                        if(navigationItem.route == "logout"){
                            authViewModel.signOut()
                            onLogOut()
                        } else {
                            navController.navigate(navigationItem.route)
                        }
                    }
                }
            )
        }
    ){
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    title = {
                        TopSearchBar(
                            searchQuery,
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
                    containerColor = MaterialTheme.colorScheme.primary
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
                LazyColumn(modifier = Modifier.fillMaxSize()){
                    items(state.notes){note->
                        NoteItem(
                            note = note,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(
                                        Screen.AddEditNoteScreen.route +
                                                "?noteId=${note.id}&noteColor=${note.color}"
                                    )
                                },
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
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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