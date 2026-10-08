package dev.samadali.zen.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    @ColumnInfo(defaultValue = "OTHER") val category: TaskCategory = TaskCategory.OTHER,
    /** Start of the due day, in wall-clock milliseconds. */
    val dueAt: Long? = null,
    /** When to send a reminder notification, if the user asked for one. */
    val remindAt: Long? = null,
    /** Order in the list, set by dragging; lower comes first. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0
)
