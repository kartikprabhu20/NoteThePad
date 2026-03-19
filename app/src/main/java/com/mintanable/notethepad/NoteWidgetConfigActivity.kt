package com.mintanable.notethepad

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mintanable.notethepad.core.model.ThemeMode
import com.mintanable.notethepad.feature_settings.SettingsViewModel
import com.mintanable.notethepad.feature_note.presentation.widget.NoteWidgetConfigScreen
import com.mintanable.notethepad.feature_widgets.presentation.utils.SingleNoteWidgetReceiver
import com.mintanable.notethepad.feature_widgets.repository.NoteWidgetPrefs
import com.mintanable.notethepad.theme.NoteThePadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state by settingsViewModel.state.collectAsStateWithLifecycle()
            val settings = state.settings
            val isDarkTheme = if(settings.themeMode == ThemeMode.SYSTEM) isSystemInDarkTheme() else settings.themeMode == ThemeMode.DARK

            NoteThePadTheme(darkTheme = isDarkTheme) {
                NoteWidgetConfigScreen(onNoteClicked = { note ->
                    note.id?.let {
                        saveAndFinish(it)
                    } ?: setResult(RESULT_CANCELED)

                })
            }
        }
    }

    private fun saveAndFinish(noteId: Long) {
        NoteWidgetPrefs.saveNoteId(this, appWidgetId, noteId)
        val verify = NoteWidgetPrefs.getNoteId(this, appWidgetId)

        val intent = Intent(this@NoteWidgetConfigActivity, SingleNoteWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        sendBroadcast(intent)

        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
        finish()
    }
}