package com.mintanable.notethepad.feature_note.data.source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mintanable.notethepad.feature_note.domain.model.Note

@Database(
    entities = [Note::class],
    version=2
)
abstract class NoteDatabase:RoomDatabase() {
    abstract  val noteDao:NoteDao

    companion object{
        const val DATABASE_NAME = "notes_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Note_title ON Note(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Note_timestamp ON Note(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Note_color ON Note(color)")
            }
        }
    }
}
