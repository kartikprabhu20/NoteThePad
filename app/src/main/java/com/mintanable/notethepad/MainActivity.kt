package com.mintanable.notethepad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mintanable.notethepad.feature_firebase.presentation.components.LoginScreen
import com.mintanable.notethepad.feature_firebase.presentation.components.SignUpScreen
import com.mintanable.notethepad.ui.util.Screen
import com.mintanable.notethepad.feature_note.presentation.modify.AddEditNoteScreen
import com.mintanable.notethepad.feature_note.presentation.modify.components.NotesScreen
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NoteThePadTheme {
                MainScreen()
            }
        }
    }


    @Composable
    fun MainScreen() {

        Surface(
            color = MaterialTheme.colors.background
        ) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.NotesScreen.route
            ) {
                composable(route = Screen.NotesScreen.route) {
                    NotesScreen(navController = navController)
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
                    LoginScreen(navController = navController)
                }
                composable(route = Screen.FirebaseSignUpScreen.route) {
                    SignUpScreen(navController = navController)
                }
            }
        }
    }

    @Preview
    @Composable
    fun PreviewConversation() {
        NoteThePadTheme {
            MainScreen()
        }
    }
}