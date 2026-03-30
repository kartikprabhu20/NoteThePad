package com.mintanable.notethepad.database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteTagCrossRef
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.dao.NoteDao
import com.mintanable.notethepad.database.db.dao.TagDao
import com.mintanable.notethepad.database.db.util.NoteConverters

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class
    ],
    version = 12
)
@TypeConverters(NoteConverters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "notes_db"

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE NoteEntity ADD COLUMN audioTranscriptions TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE NoteEntity ADD COLUMN backgroundImage INTEGER NOT NULL DEFAULT -1")
            }
        }
    }
}
