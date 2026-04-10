package com.mintanable.notethepad.database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mintanable.notethepad.database.db.entity.CollaboratorEntity
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteTagCrossRef
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.dao.CollaboratorDao
import com.mintanable.notethepad.database.db.dao.NoteDao
import com.mintanable.notethepad.database.db.dao.TagDao
import com.mintanable.notethepad.database.db.util.NoteConverters

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        CollaboratorEntity::class
    ],
    version = 17
)
@TypeConverters(NoteConverters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun collaboratorDao(): CollaboratorDao

    companion object {
        const val DATABASE_NAME = "notes_db"

    }
}