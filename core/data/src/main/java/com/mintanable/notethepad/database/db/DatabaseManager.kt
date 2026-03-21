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
            NoteDatabase.DATABASE_NAME
        )
            .build()
    }

    fun swapDatabase(tempFile: File) {
        _database?.close()
        _database = null

        val dbFile = context.getDatabasePath(NoteDatabase.DATABASE_NAME)

        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")

        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        tempFile.copyTo(dbFile, overwrite = true)
        tempFile.delete()
    }
}
