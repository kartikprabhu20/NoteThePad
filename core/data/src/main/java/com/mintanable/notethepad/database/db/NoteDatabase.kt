package com.mintanable.notethepad.database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. TagEntity Migration
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tag_table_new` (
                        `tagName` TEXT NOT NULL, 
                        `tagId` TEXT NOT NULL, 
                        `lastUpdateTime` INTEGER NOT NULL, 
                        `userId` TEXT, 
                        `isDeleted` INTEGER NOT NULL, 
                        PRIMARY KEY(`tagId`)
                    )
                """)
                database.execSQL("""
                    INSERT INTO tag_table_new (tagName, tagId, lastUpdateTime, userId, isDeleted)
                    SELECT tagName, CAST(tagId AS TEXT), strftime('%s','now')*1000, NULL, 0 FROM tag_table
                """)
                database.execSQL("DROP TABLE tag_table")
                database.execSQL("ALTER TABLE tag_table_new RENAME TO tag_table")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tag_table_tagName` ON `tag_table` (`tagName`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tag_table_userId` ON `tag_table` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tag_table_isDeleted` ON `tag_table` (`isDeleted`)")

                // 2. NoteEntity Migration
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `NoteEntity_new` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `content` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `color` INTEGER NOT NULL, 
                        `imageUris` TEXT NOT NULL, 
                        `audioUris` TEXT NOT NULL, 
                        `reminderTime` INTEGER NOT NULL, 
                        `audioTranscriptions` TEXT NOT NULL, 
                        `backgroundImage` INTEGER NOT NULL, 
                        `lastUpdateTime` INTEGER NOT NULL, 
                        `userId` TEXT, 
                        `isDeleted` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
                database.execSQL("""
                    INSERT INTO NoteEntity_new (id, title, content, timestamp, color, imageUris, audioUris, reminderTime, audioTranscriptions, backgroundImage, lastUpdateTime, userId, isDeleted)
                    SELECT CAST(id AS TEXT), title, content, timestamp, color, imageUris, audioUris, reminderTime, audioTranscriptions, backgroundImage, strftime('%s','now')*1000, NULL, 0 FROM NoteEntity
                """)
                database.execSQL("DROP TABLE NoteEntity")
                database.execSQL("ALTER TABLE NoteEntity_new RENAME TO NoteEntity")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteEntity_title` ON `NoteEntity` (`title`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteEntity_timestamp` ON `NoteEntity` (`timestamp`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteEntity_color` ON `NoteEntity` (`color`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteEntity_reminderTime` ON `NoteEntity` (`reminderTime`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteEntity_userId` ON `NoteEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteEntity_isDeleted` ON `NoteEntity` (`isDeleted`)")

                // 3. NoteTagCrossRef Migration
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `note_tag_cross_ref_new` (
                        `noteId` TEXT NOT NULL, 
                        `tagId` TEXT NOT NULL, 
                        `userId` TEXT, 
                        `isDeleted` INTEGER NOT NULL, 
                        `lastUpdateTime` INTEGER NOT NULL, 
                        PRIMARY KEY(`noteId`, `tagId`), 
                        FOREIGN KEY(`noteId`) REFERENCES `NoteEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                        FOREIGN KEY(`tagId`) REFERENCES `tag_table`(`tagId`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                database.execSQL("""
                    INSERT INTO note_tag_cross_ref_new (noteId, tagId, userId, isDeleted, lastUpdateTime)
                    SELECT CAST(noteId AS TEXT), CAST(tagId AS TEXT), NULL, 0, strftime('%s','now')*1000 FROM note_tag_cross_ref
                """)
                database.execSQL("DROP TABLE note_tag_cross_ref")
                database.execSQL("ALTER TABLE note_tag_cross_ref_new RENAME TO note_tag_cross_ref")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` ON `note_tag_cross_ref` (`tagId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` ON `note_tag_cross_ref` (`noteId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_userId` ON `note_tag_cross_ref` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_isDeleted` ON `note_tag_cross_ref` (`isDeleted`)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE NoteEntity ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0")

            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tag_table ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0")

            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `note_collaborators_local` (
                        `id` TEXT NOT NULL,
                        `noteId` TEXT NOT NULL,
                        `ownerUserId` TEXT NOT NULL,
                        `collaboratorUserId` TEXT NOT NULL,
                        `collaboratorEmail` TEXT NOT NULL,
                        `collaboratorDisplayName` TEXT,
                        `collaboratorPhotoUrl` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_collaborators_local_noteId` ON `note_collaborators_local` (`noteId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_collaborators_local_collaboratorUserId` ON `note_collaborators_local` (`collaboratorUserId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_collaborators_local_ownerUserId` ON `note_collaborators_local` (`ownerUserId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_note_collaborators_local_noteId_collaboratorUserId` ON `note_collaborators_local` (`noteId`, `collaboratorUserId`)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE NoteEntity ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
