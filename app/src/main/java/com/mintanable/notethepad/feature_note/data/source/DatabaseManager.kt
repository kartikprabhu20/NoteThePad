package com.mintanable.notethepad.feature_note.data.source

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _database: NoteDatabase? = null

    val database: NoteDatabase
        get() {
            if (_database == null || !_database!!.isOpen) {
                _database = buildDatabase()
            }
            return _database!!
        }

    private fun buildDatabase(): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            NoteDatabase.DATABASE_NAME
        ).build()
    }

    fun swapDatabase(tempFile: File) {
        _database?.close()
        _database = null

        val dbFile = context.getDatabasePath(NoteDatabase.DATABASE_NAME)
        tempFile.copyTo(dbFile, overwrite = true)
        tempFile.delete()

        // Re-initialization happens automatically next time 'database' is accessed
    }
}