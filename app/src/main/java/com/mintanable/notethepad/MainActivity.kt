package com.mintanable.notethepad

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mintanable.notethepad.feature_backup.presentation.RestoreEvent
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthEvent
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel
import com.mintanable.notethepad.feature_firebase.presentation.auth.GoogleClientHelper
import com.mintanable.notethepad.feature_firebase.presentation.components.LoginScreen
import com.mintanable.notethepad.feature_note.data.repository.AndroidMediaPlayer
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteScreen
import com.mintanable.notethepad.feature_note.presentation.notes.NotesScreen
import com.mintanable.notethepad.feature_note.presentation.notes.util.ReminderReceiver.Companion.LAUNCH_EDIT_SCREEN
import com.mintanable.notethepad.feature_note.presentation.notes.util.ReminderReceiver.Companion.TARGET_NOTE_ID
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.SettingsEvent
import com.mintanable.notethepad.feature_settings.presentation.SettingsScreen
import com.mintanable.notethepad.feature_settings.presentation.SettingsViewModel
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.util.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var credentialHelper: GoogleClientHelper
    private var intentState = mutableStateOf<Intent?>(null)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent

        credentialHelper = GoogleClientHelper(this)
        setContent {

            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state by settingsViewModel.state.collectAsStateWithLifecycle()
            val isDarkTheme = if(state.settings.themeMode == ThemeMode.SYSTEM) isSystemInDarkTheme() else state.settings.themeMode == ThemeMode.DARK
            val currentIntent by intentState

            NoteThePadTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    LaunchedEffect(currentIntent) {
                        val noteId = currentIntent?.getLongExtra(TARGET_NOTE_ID, -1L) ?: -1L
                        if (noteId != -1L) {
                            navController.navigate(Screen.AddEditNoteScreen.passArgs(noteId=noteId)) {
                                launchSingleTop = true
                            }
                            currentIntent?.removeExtra(TARGET_NOTE_ID)
                        }
                        val launchEditScreen = currentIntent?.getBooleanExtra(LAUNCH_EDIT_SCREEN, false) ?: false
                        if(launchEditScreen){
                            navController.navigate(Screen.AddEditNoteScreen.route)
                            currentIntent?.removeExtra(LAUNCH_EDIT_SCREEN)
                        }
                    }
                    val showToast = { message: String ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->
                            if (result.resultCode == RESULT_OK) {
                                settingsViewModel.onEvent(SettingsEvent.AuthResultCompleted(
                                    intent = result.data,
                                    onFailure = showToast
                                ))
                            } else {
                                settingsViewModel.onEvent(SettingsEvent.AuthCancelled)
                            }
                        }
                    )

                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.NotesScreen.route
                        ) {
                            composable(
                                route = Screen.NotesScreen.route,
                                arguments = listOf(
                                    navArgument("tagId") { type = NavType.LongType; defaultValue = -1L },
                                    navArgument("tagName") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("filterType") { type = NavType.StringType; defaultValue = "all" }
                                )
                            ) { entry ->
                                NotesScreen(
                                    navController = navController,
                                    onLogOut = {
                                        credentialHelper.clearCredentials()
                                        settingsViewModel.onEvent(SettingsEvent.SignOut)
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable
                                )
                            }
                            composable(
                                route = Screen.AddEditNoteScreen.route,
                                arguments = listOf(
                                    navArgument(name = "noteId") { type = NavType.LongType; defaultValue = -1L }
                                )
                            ) {
                                AddEditNoteScreen(
                                    noteId = it.arguments?.getLong("noteId") ?: 0L,
                                    navController = navController,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable
                                )
                            }
                            composable(route = Screen.FirebaseLoginScreen.route) {
                                val viewModel: AuthViewModel = hiltViewModel()

                                LoginScreen(
                                    navController = navController,
                                    onGoogleSigInClick = {
                                        lifecycleScope.launch {
                                            val token = credentialHelper.getGoogleCredential()
                                            if (token != null) {
                                                viewModel.onEvent(AuthEvent.GoogleSignIn(token))
                                            }
                                        }
                                    },
                                    onFacebookSignInClick = {

                                    }
                                )
                            }
                            composable(route = Screen.SettingsScreen.route) {
                                LaunchedEffect(Unit) {
                                    settingsViewModel.restoreEvents.collect { event ->
                                        when (event) {
                                            is RestoreEvent.NavigateToHome -> {
                                                navController.navigate(Screen.NotesScreen.route) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                }

                                SettingsScreen(
                                    state = state,
                                    onBackPressed = {
                                        navController.navigateUp()
                                    },
                                    onEvent = { event ->
                                        if (event is SettingsEvent.UpdateBackupSettings) {
                                            settingsViewModel.onEvent(event.copy(onAuthRequired = { pendingIntent ->
                                                launcher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                                            }))
                                        } else {
                                            settingsViewModel.onEvent(event)
                                        }
                                    },
                                    showToast = showToast
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isFinishing){
            AndroidMediaPlayer(this).release()
        }
    }
}
