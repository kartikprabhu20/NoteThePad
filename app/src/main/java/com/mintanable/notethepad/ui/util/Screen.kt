package com.mintanable.notethepad.ui.util

sealed class Screen(val route:String){
    object NotesScreen: Screen("notes_screen")
    object AddEditNoteScreen : Screen("add_edit_note_screen")
    object FirebaseLoginScreen : Screen("firebase_login_screen")
    object FirebaseSignUpScreen : Screen("firebase_signup_screen")
}
