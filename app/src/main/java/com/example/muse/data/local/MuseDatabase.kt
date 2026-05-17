package com.example.muse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DocumentEntity::class,
        BlockEntity::class,
        PrimaryTagEntity::class,
        SecondaryTagEntity::class,
        DocumentSecondaryTagCrossRef::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MuseDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun blockDao(): BlockDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: MuseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `primary_tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_primary_tags_name` ON `primary_tags` (`name`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `secondary_tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_secondary_tags_name` ON `secondary_tags` (`name`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `document_secondary_tag` (
                        `documentId` INTEGER NOT NULL,
                        `secondaryTagId` INTEGER NOT NULL,
                        PRIMARY KEY(`documentId`, `secondaryTagId`),
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`secondaryTagId`) REFERENCES `secondary_tags`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_document_secondary_tag_secondaryTagId` ON `document_secondary_tag` (`secondaryTagId`)")
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `primary_tag_id` INTEGER REFERENCES `primary_tags`(`id`)")
            }
        }

        fun getInstance(context: Context): MuseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MuseDatabase::class.java,
                    "muse.db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
