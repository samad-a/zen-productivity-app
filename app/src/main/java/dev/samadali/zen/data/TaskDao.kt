package dev.samadali.zen.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    /** Unfinished tasks in the user's order, then finished tasks, most recent first. */
    @Query(
        "SELECT * FROM tasks ORDER BY isCompleted, " +
            "CASE WHEN isCompleted THEN -completedAt ELSE position END, createdAt"
    )
    fun getAll(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    /** Unfinished tasks with a reminder still to come, to reschedule after a reboot. */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND remindAt > :now")
    suspend fun getUpcomingReminders(now: Long): List<Task>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE isCompleted = 0")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(task: Task): Long

    @Insert
    suspend fun insertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun updateAll(tasks: List<Task>)

    @Delete
    suspend fun delete(task: Task)

    @Delete
    suspend fun deleteAll(tasks: List<Task>)
}
