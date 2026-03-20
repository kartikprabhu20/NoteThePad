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
    version = 10
)
@TypeConverters(NoteConverters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val tagDao: TagDao

    companion object {
        const val DATABASE_NAME = "notes_db"
    }
}
