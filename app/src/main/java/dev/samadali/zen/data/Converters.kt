package dev.samadali.zen.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun categoryToName(category: TaskCategory): String = category.name

    /** Unknown names (e.g. from a newer version restored from backup) fall back to OTHER. */
    @TypeConverter
    fun nameToCategory(name: String): TaskCategory =
        TaskCategory.entries.firstOrNull { it.name == name } ?: TaskCategory.OTHER
}
