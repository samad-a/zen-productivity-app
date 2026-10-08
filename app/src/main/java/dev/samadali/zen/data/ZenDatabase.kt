package dev.samadali.zen.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version history:
 * 1. tasks
 * 2. focus_sessions, for the session counter, stats, streaks and calendar
 * 3. tasks: category, dueAt, remindAt and position
 */
@Database(
    entities = [Task::class, FocusSession::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)]
)
@TypeConverters(Converters::class)
abstract class ZenDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val NAME = "zen.db"

        fun create(context: Context): ZenDatabase =
            Room.databaseBuilder(context, ZenDatabase::class.java, NAME).build()
    }
}
