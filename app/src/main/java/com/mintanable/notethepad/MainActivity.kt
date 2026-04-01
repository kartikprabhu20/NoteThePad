package com.mintanable.notethepad

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mintanable.notethepad.core.common.Screen
import com.mintanable.notethepad.core.model.settings.ThemeMode
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthEvent
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel
import com.mintanable.notethepad.feature_firebase.GoogleClientHelper
import com.mintanable.notethepad.feature_firebase.presentation.components.LoginScreen
import com.mintanable.notethepad.feature_note.domain.repository.MediaPlayer
import com.mintanable.notethepad.feature_widgets.presentation.utils.SingleNoteWidgetReceiver
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteScreen
import com.mintanable.notethepad.feature_note.presentation.notes.NotesScreen
import com.mintanable.notethepad.feature_calendar.CalendarScreen
import com.mintanable.notethepad.core.common.NavigationConstants
import com.mintanable.notethepad.feature_settings.SettingsViewModel
import com.mintanable.notethepad.feature_settings.presentation.SettingsEvent
import com.mintanable.notethepad.feature_settings.presentation.SettingsScreen
import com.mintanable.notethepad.feature_note.presentation.archive.ArchiveScreen
import com.mintanable.notethepad.theme.NoteThePadTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var mediaPlayer: MediaPlayer

    private lateinit var credentialHelper: GoogleClientHelper
    private var intentState = mutableStateOf<Intent?>(null)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent

        credentialHelper = GoogleClientHelper(this, this.getString(R.string.default_web_client_id))
        setContent {

            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state by settingsViewModel.state.collectAsStateWithLifecycle()
            val isDarkTheme = if (state.settings.themeMode == ThemeMode.SYSTEM) isSystemInDarkTheme() else state.settings.themeMode == ThemeMode.DARK
            val currentIntent by intentState

            NoteThePadTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    LaunchedEffect(currentIntent) {
                        val noteId = currentIntent?.getStringExtra(NavigationConstants.EXTRA_NOTE_ID)
                        if (!noteId.isNullOrBlank()) {
                            navController.navigate(Screen.AddEditNoteScreen.passArgs(noteId = noteId)) {
                                launchSingleTop = true
                            }
                            currentIntent?.removeExtra(NavigationConstants.EXTRA_NOTE_ID)
                        }
                        val launchEditScreen = currentIntent?.getBooleanExtra(NavigationConstants.LAUNCH_EDIT_SCREEN, false) ?: false
                        if(launchEditScreen){
                            navController.navigate(Screen.AddEditNoteScreen.passArgs())
                            currentIntent?.removeExtra(NavigationConstants.LAUNCH_EDIT_SCREEN)
                        }
                    }
                    val showToast = { message: String ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->
                            if (result.resultCode == RESULT_OK) {
                                settingsViewModel.onEvent(
                                    SettingsEvent.AuthResultCompleted(
                                        intent = result.data,
                                        onFailure = showToast
                                    )
                                )
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
                                    navArgument("tagId") {
                                        type = NavType.StringType; defaultValue = ""
                                    },
                                    navArgument("tagName") {
                                        type = NavType.StringType; defaultValue = ""
                                    },
                                    navArgument("filterType") {
                                        type = NavType.StringType; defaultValue = "all"
                                    }
                                )
                            ) {

                                val authViewModel: AuthViewModel = hiltViewModel()
                                val user by authViewModel.currentUser.collectAsStateWithLifecycle()
                                val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

                                NotesScreen(
                                    navController = navController,
                                    onLogOut = {
                                        authViewModel.signOut()
                                        credentialHelper.clearCredentials()
                                        settingsViewModel.onEvent(SettingsEvent.SignOut)
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    user = user,
                                    isDarkTheme = isDarkTheme,
                                    appVersionProvider = AndroidAppVersionProvider(),
                                    onPinWidget = { note ->
                                        val appWidgetManager =
                                            AppWidgetManager.getInstance(this@MainActivity)
                                        val provider = ComponentName(
                                            this@MainActivity,
                                            SingleNoteWidgetReceiver::class.java
                                        )
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            val callback = Intent(
                                                this@MainActivity,
                                                SingleNoteWidgetReceiver::class.java
                                            ).apply {
                                                putExtra(
                                                    SingleNoteWidgetReceiver.PINNING_NOTE_ID,
                                                    note.id
                                                )
                                                action = SingleNoteWidgetReceiver.PINNING_ACTION
                                            }
                                            val pendingIntent = PendingIntent.getBroadcast(
                                                this@MainActivity, note.id.hashCode(), callback,
                                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                            )
                                            appWidgetManager.requestPinAppWidget(
                                                provider,
                                                null,
                                                pendingIntent
                                            )
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Pinned widgets are not supported on this launcher",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                            composable(
                                route = Screen.AddEditNoteScreen.route,
                                arguments = listOf(
                                    navArgument(name = "noteId") {
                                        type = NavType.StringType; defaultValue = ""
                                    },
                                    navArgument(name = "reminderTime") {
                                        type = NavType.LongType; defaultValue = -1L
                                    },
                                    navArgument(name = "initialTitle") {
                                        type = NavType.StringType; nullable = true; defaultValue = null
                                    }
                                )
                            ) {
                                AddEditNoteScreen(
                                    noteId = it.arguments?.getString("noteId") ?: "",
                                    isDarkTheme = isDarkTheme,
                                    navController = navController,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    onPinWidget = { noteId ->
                                        val appWidgetManager =
                                            AppWidgetManager.getInstance(this@MainActivity)
                                        val provider = ComponentName(
                                            this@MainActivity,
                                            SingleNoteWidgetReceiver::class.java
                                        )
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            val callback = Intent(
                                                this@MainActivity,
                                                SingleNoteWidgetReceiver::class.java
                                            ).apply {
                                                putExtra(
                                                    SingleNoteWidgetReceiver.PINNING_NOTE_ID,
                                                    noteId
                                                )
                                                action = SingleNoteWidgetReceiver.PINNING_ACTION
                                            }
                                            val pendingIntent = PendingIntent.getBroadcast(
                                                this@MainActivity, noteId.hashCode(), callback,
                                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                            )
                                            appWidgetManager.requestPinAppWidget(
                                                provider,
                                                null,
                                                pendingIntent
                                            )
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Pinned widgets are not supported on this launcher",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                            composable(route = Screen.FirebaseLoginScreen.route) {
                                val viewModel: AuthViewModel = hiltViewModel()

                                LoginScreen(
                                    navController = navController,
                                    currentTheme = state.settings.themeMode,
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
                                SettingsScreen(
                                    state = state,
                                    onBackPressed = {
                                        navController.navigateUp()
                                    },
                                    onEvent = { event ->
                                        if (event is SettingsEvent.UpdateBackupSettings) {
                                            settingsViewModel.onEvent(event.copy(onAuthRequired = { pendingIntent ->
                                                launcher.launch(
                                                    IntentSenderRequest.Builder(
                                                        pendingIntent
                                                    ).build()
                                                )
                                            }))
                                        } else {
                                            settingsViewModel.onEvent(event)
                                        }
                                    },
                                    showToast = showToast
                                )
                            }
                            composable(route = Screen.CalendarScreen.route) {
                                CalendarScreen(navController = navController)
                            }
                            composable(route = Screen.ArchiveScreen.route) {
                                ArchiveScreen(
                                    onBackPressed = { navController.navigateUp() },
                                    isDarkTheme = isDarkTheme,
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
        if (isFinishing) {
            mediaPlayer.release()
        }
    }
}
