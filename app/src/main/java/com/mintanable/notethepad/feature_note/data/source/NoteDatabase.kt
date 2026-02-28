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
    version = 5
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Note ADD COLUMN audioUris TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create a new temporary table with the EXACT schema Room expects
                // Note: No 'DEFAULT -1' here, as Room expects 'undefined'
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `Note_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
                `title` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `color` INTEGER NOT NULL, 
                `imageUris` TEXT NOT NULL, 
                `audioUris` TEXT NOT NULL, 
                `reminderTime` INTEGER NOT NULL
            )
        """.trimIndent())

                // 2. Copy the data from the old table to the new one
                // We set -1 as the value for the new column during the copy
                db.execSQL("""
            INSERT INTO `Note_new` (id, title, content, timestamp, color, imageUris, audioUris, reminderTime)
            SELECT id, title, content, timestamp, color, imageUris, audioUris, -1 FROM Note
        """.trimIndent())

                // 3. Remove the old table
                db.execSQL("DROP TABLE Note")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE Note_new RENAME TO Note")

                // 5. Re-create all the indices Room expects
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_title` ON `Note` (`title`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_timestamp` ON `Note` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_color` ON `Note` (`color`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_reminderTime` ON `Note` (`reminderTime`)")
            }
        }
    }
}
