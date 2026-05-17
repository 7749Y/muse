package com.example.muse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, BlockEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MuseDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun blockDao(): BlockDao

    companion object {
        @Volatile
        private var INSTANCE: MuseDatabase? = null

        fun getInstance(context: Context): MuseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MuseDatabase::class.java,
                    "muse.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
