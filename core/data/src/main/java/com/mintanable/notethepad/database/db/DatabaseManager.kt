package com.mintanable.notethepad.database.db

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val mutex = Mutex()
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
            .addMigrations(NoteDatabase.MIGRATION_10_11, NoteDatabase.MIGRATION_11_12)
            .build()
    }

    fun swapDatabase(tempFile: File) {
        _database?.close()
        _database = null

        val dbFile = context.getDatabasePath(NoteDatabase.DATABASE_NAME)

        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")

        if (dbFile.exists()) dbFile.delete()
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        tempFile.copyTo(dbFile, overwrite = true)
        tempFile.delete()
    }

    fun getDatabaseFile(): File {
        return context.getDatabasePath(NoteDatabase.DATABASE_NAME)
    }

    //To force WAL merge into DB
    fun closeDatabase() {
        synchronized(this) {
            _database?.close()
            _database = null
        }
    }
}
