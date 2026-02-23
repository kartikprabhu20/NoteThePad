package com.mintanable.notethepad

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.mintanable.notethepad.ui.util.Screen
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteScreen
import com.mintanable.notethepad.feature_note.presentation.modify.components.NotesScreen
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.SettingsScreen
import com.mintanable.notethepad.feature_settings.presentation.SettingsViewModel
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var credentialHelper: GoogleClientHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        credentialHelper = GoogleClientHelper(this)
        setContent {

            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
            val isDarkTheme = if(settings.themeMode == ThemeMode.SYSTEM) isSystemInDarkTheme() else settings.themeMode == ThemeMode.DARK
            NoteThePadTheme(darkTheme = isDarkTheme) {
                MainScreen(settingsViewModel, settings)
            }
        }
    }

    @Composable
    fun MainScreen(settingsViewModel: SettingsViewModel, settings: Settings) {

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                settingsViewModel.onAuthResultCompleted(result.data, { error -> showToast(error) } )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            SharedTransitionLayout {

                NavHost(
                    navController = navController,
                    startDestination = Screen.NotesScreen.route
                ) {
                    composable(route = Screen.NotesScreen.route) {
                        NotesScreen(
                            navController = navController,
                            onLogOut =
                                {
                                    credentialHelper.clearCredentials()
                                    settingsViewModel.signOut()
                                },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable(
                        route = Screen.AddEditNoteScreen.route + "?noteId={noteId}&noteColor={noteColor}",
                        arguments = listOf(
                            navArgument(
                                name = "noteId"
                            ) {
                                type = NavType.IntType
                                defaultValue = -1
                            },
                            navArgument(
                                name = "noteColor"
                            ) {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) {
                        val color = it.arguments?.getInt("noteColor") ?: -1
                        AddEditNoteScreen(
                            noteId = it.arguments?.getInt("noteId"),
                            navController = navController,
                            noteColor = color,
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
                        val isAuthorisingBackup by settingsViewModel.isAuthorisingBackup.collectAsStateWithLifecycle()
                        val backupUiState by settingsViewModel.backupUiState.collectAsStateWithLifecycle()
                        val backupUploadDownloadState by settingsViewModel.backupUploadDownloadState.collectAsStateWithLifecycle()

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
                            onBackPressed = {
                                navController.navigateUp()
                            },
                            isAuthorisingBackup = isAuthorisingBackup,
                            currentSettings = settings,
                            backupUploadDownloadState = backupUploadDownloadState,
                            backupUiState = backupUiState,
                            onLoadBackupInfo = { settingsViewModel.loadBackupInfo() },
                            onThemeChanged = { theme ->
                                settingsViewModel.updateTheme(theme)
                            },
                            onBackupSettingsChanged = { backupEnabled ->
                                settingsViewModel.toggleBackup(
                                    backupEnabled,
                                    { pendingIntent ->
                                        launcher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                                    },
                                    { error ->
                                        showToast(error)
                                    }
                                )
                            },
                            onBackupIntervalChanged = { backupFrequency ->
                                settingsViewModel.updateBackupSettings(backupFrequency, settings.backupTimeHour, settings.backupTimeMinutes)
                            },
                            onBackupTimeChanged = { hours,minutes ->
                                settingsViewModel.updateBackupSettings(settings.backupFrequency, hours, minutes)
                            },
                            onBackupNowClicked = {
                                settingsViewModel.updateBackupSettings(settings.backupFrequency, settings.backupTimeHour, settings.backupTimeMinutes, true)
                            },
                            onRestoreClicked = {
                                settingsViewModel.startRestore()
                            },
                            showToast = { message ->
                                showToast(message)
                            },
                            onDummyDataCreate = {
                                settingsViewModel.createMassiveDummyData()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this@MainActivity,
            message,
            Toast.LENGTH_LONG
        )
            .show()
    }
}
