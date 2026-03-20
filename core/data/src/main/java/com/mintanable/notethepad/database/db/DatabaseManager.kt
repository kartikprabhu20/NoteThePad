package com.mintanable.notethepad.database.db

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
            NoteDatabase.Companion.DATABASE_NAME
        )
            .addMigrations(NoteDatabase.Companion.MIGRATION_1_2)
            .addMigrations(NoteDatabase.Companion.MIGRATION_2_3)
            .addMigrations(NoteDatabase.Companion.MIGRATION_3_4)
            .addMigrations(NoteDatabase.Companion.MIGRATION_4_5)
            .addMigrations(NoteDatabase.Companion.MIGRATION_5_6)
            .addMigrations(NoteDatabase.Companion.MIGRATION_6_7)
            .addMigrations(NoteDatabase.Companion.MIGRATION_7_8)
            .addMigrations(NoteDatabase.Companion.MIGRATION_8_9)
            .build()
    }

    fun swapDatabase(tempFile: File) {
        _database?.close()
        _database = null

        val dbFile = context.getDatabasePath(NoteDatabase.Companion.DATABASE_NAME)
        tempFile.copyTo(dbFile, overwrite = true)
        tempFile.delete()

        // Re-initialization happens automatically next time 'database' is accessed
    }
}
