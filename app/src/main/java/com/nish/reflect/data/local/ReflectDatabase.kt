package com.nish.reflect.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nish.reflect.data.model.JournalEntry
import com.nish.reflect.data.model.MoodLog

@Database(
    entities = [JournalEntry::class, MoodLog::class],
    version = 1,
    exportSchema = false
)
abstract class ReflectDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun moodLogDao(): MoodLogDao

    companion object {
        @Volatile
        private var INSTANCE: ReflectDatabase? = null

        fun getInstance(context: Context): ReflectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReflectDatabase::class.java,
                    "reflect.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}