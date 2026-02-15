package com.mintanable.notethepad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthEvent
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel
import com.mintanable.notethepad.feature_firebase.presentation.auth.GoogleClientHelper
import com.mintanable.notethepad.feature_firebase.presentation.components.LoginScreen
import com.mintanable.notethepad.ui.util.Screen
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteScreen
import com.mintanable.notethepad.feature_note.presentation.modify.components.NotesScreen
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
            NoteThePadTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {

        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.NotesScreen.route
            ) {
                composable(route = Screen.NotesScreen.route) {
                    NotesScreen(navController = navController, onLogOut = {
                        credentialHelper.clearCredentials()
                    })
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
                        navController = navController,
                        noteColor = color
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
            }
        }
    }
}
