package com.mintanable.notethepad.feature_note.data.source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.util.NoteConverters

@Database(
    entities = [Note::class],
    version=3
)
@TypeConverters(NoteConverters::class)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Note ADD COLUMN imageUris TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
