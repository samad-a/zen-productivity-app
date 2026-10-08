package dev.samadali.zen.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A completed pomodoro study session. Only sessions that run to the end are logged;
 * skipped or reset sessions are not. Times are wall-clock milliseconds.
 */
@Entity(tableName = "focus_sessions", indices = [Index("completedAt")])
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val completedAt: Long,
    /** Planned length of the session; time spent paused isn't counted. */
    val durationMillis: Long
)
