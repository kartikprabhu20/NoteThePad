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
    version = 9
)
@TypeConverters(NoteConverters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val tagDao: TagDao

    companion object {
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

                db.execSQL("""
            INSERT INTO `Note_new` (id, title, content, timestamp, color, imageUris, audioUris, reminderTime)
            SELECT id, title, content, timestamp, color, imageUris, audioUris, -1 FROM Note
        """.trimIndent())

                db.execSQL("DROP TABLE Note")
                db.execSQL("ALTER TABLE Note_new RENAME TO Note")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_title` ON `Note` (`title`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_timestamp` ON `Note` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_color` ON `Note` (`color`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_reminderTime` ON `Note` (`reminderTime`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tag_table` (`tagName` TEXT NOT NULL, PRIMARY KEY(`tagName`))")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (
                `noteId` INTEGER NOT NULL, 
                `tagName` TEXT NOT NULL, 
                PRIMARY KEY(`noteId`, `tagName`), 
                FOREIGN KEY(`noteId`) REFERENCES `Note`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`tagName`) REFERENCES `tag_table`(`tagName`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagName` ON `note_tag_cross_ref` (`tagName`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tag_table_new` (`tagId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tagName` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tag_table_tagName` ON `tag_table_new` (`tagName`)")

                db.execSQL("INSERT INTO `tag_table_new` (`tagName`) SELECT `tagName` FROM `tag_table`")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `note_tag_cross_ref_new` (
                `noteId` INTEGER NOT NULL, 
                `tagId` INTEGER NOT NULL, 
                PRIMARY KEY(`noteId`, `tagId`), 
                FOREIGN KEY(`noteId`) REFERENCES `Note`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`tagId`) REFERENCES `tag_table`(`tagId`) ON UPDATE NO ACTION ON DELETE CASCADE)""")

                db.execSQL("""INSERT INTO `note_tag_cross_ref_new` (`noteId`, `tagId`)
                SELECT ref.noteId, newTag.tagId
                FROM note_tag_cross_ref AS ref
                JOIN tag_table_new AS newTag ON ref.tagName = newTag.tagName""")

                db.execSQL("DROP TABLE `tag_table`")
                db.execSQL("DROP TABLE `note_tag_cross_ref`")

                db.execSQL("ALTER TABLE `tag_table_new` RENAME TO `tag_table`")
                db.execSQL("ALTER TABLE `note_tag_cross_ref_new` RENAME TO `note_tag_cross_ref`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` ON `note_tag_cross_ref` (`tagId`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `Note_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `color` INTEGER NOT NULL,
                `reminderTime` INTEGER NOT NULL,
                `imageUris` TEXT NOT NULL,
                `audioUris` TEXT NOT NULL)""")

                db.execSQL("""INSERT INTO `Note_new` (`id`, `title`, `content`, `timestamp`, `color`, `reminderTime`, `imageUris`, `audioUris`)
                SELECT `id`, `title`, `content`, `timestamp`, `color`, 
                   COALESCE(`reminderTime`, 0), 
                   COALESCE(`imageUris`, ''), 
                   COALESCE(`audioUris`, '') 
                FROM `Note`""")

                db.execSQL("DROP TABLE `note`")
                db.execSQL("ALTER TABLE `note_new` RENAME TO `note`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_timestamp` ON `Note` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_color` ON `Note` (`color`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_reminderTime` ON `Note` (`reminderTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Note_title` ON `Note` (`title`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` ON `note_tag_cross_ref` (`noteId`)")
            }
        }
    }
}
