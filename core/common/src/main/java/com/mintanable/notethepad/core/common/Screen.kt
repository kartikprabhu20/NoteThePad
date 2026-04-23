package com.mintanable.notethepad.core.common

sealed class Screen(val route:String){
    data object NotesScreen : Screen("notes_screen?tagId={tagId}&tagName={tagName}&filterType={filterType}") {
        fun passArgs(
            tagId: String? = null,
            tagName: String? = null,
            filterType: String? = null
        ): String {
            return "notes_screen?tagId=${tagId ?: ""}&tagName=${tagName ?: ""}&filterType=${filterType ?: NotesFilterType.ALL.filter}"
        }
    }
    data object AddEditNoteScreen : Screen(
        "add_edit_note_screen?noteId={noteId}&reminderTime={reminderTime}&initialTitle={initialTitle}"
    ) {
        fun passArgs(
            noteId: String? = null,
            reminderTime: Long = -1L,
            initialTitle: String? = null
        ): String {
            val encodedTitle = if (!initialTitle.isNullOrBlank()) {
                android.net.Uri.encode(initialTitle)
            } else null

            val baseRoute = "add_edit_note_screen?noteId=${noteId ?: ""}&reminderTime=$reminderTime"
            return if (encodedTitle != null) {
                "$baseRoute&initialTitle=$encodedTitle"
            } else {
                baseRoute
            }
        }
    }

    data object CalendarScreen : Screen("calendar_screen")
    data object ArchiveScreen : Screen("archive_screen")

    data object FirebaseLoginScreen : Screen("firebase_login_screen")
    data object LogOut : Screen("logout")
    data object SettingsScreen : Screen("settings_screen")
    data object HelpAndFeedbackScreen : Screen("help_and_feedback_screen")
    data object AiModelSelectionScreen : Screen("ai_model_selection_screen")
    data object OnboardingScreen : Screen("onboarding_screen")
}
