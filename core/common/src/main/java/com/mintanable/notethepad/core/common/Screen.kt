package com.mintanable.notethepad.core.common

sealed class Screen(val route:String){
    data object NotesScreen : Screen("notes_screen?tagId={tagId}&tagName={tagName}&filterType={filterType}") {
        fun passArgs(
            tagId: Long? = null,
            tagName: String? = null,
            filterType: String? = null
        ): String {
            return "notes_screen?tagId=${tagId ?: -1L}&tagName=${tagName ?: ""}&filterType=${filterType ?: NotesFilterType.ALL.filter}"
        }
    }
    data object AddEditNoteScreen : Screen(
        "add_edit_note_screen?noteId={noteId}&reminderTime={reminderTime}&initialTitle={initialTitle}"
    ) {
        fun passArgs(
            noteId: Long? = null,
            reminderTime: Long = -1L,
            initialTitle: String = ""
        ): String {
            val encodedTitle = android.net.Uri.encode(initialTitle)
            return "add_edit_note_screen?noteId=${noteId ?: -1L}&reminderTime=$reminderTime&initialTitle=$encodedTitle"
        }
    }

    data object CalendarScreen : Screen("calendar_screen")

    data object FirebaseLoginScreen : Screen("firebase_login_screen")
    data object LogOut : Screen("logout")
    data object SettingsScreen : Screen("settings_screen")
}
