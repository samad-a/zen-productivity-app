package dev.samadali.zen

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class ZenDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        fun create(context: Context): ZenDatabase =
            Room.databaseBuilder(context, ZenDatabase::class.java, "zen.db").build()
    }
}
