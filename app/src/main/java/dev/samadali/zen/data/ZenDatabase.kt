package dev.samadali.zen.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version history:
 * 1. tasks
 * 2. focus_sessions, for the session counter, stats, streaks and calendar
 */
@Database(
    entities = [Task::class, FocusSession::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class ZenDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val NAME = "zen.db"

        fun create(context: Context): ZenDatabase =
            Room.databaseBuilder(context, ZenDatabase::class.java, NAME).build()
    }
}
