package dev.samadali.zen.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSession): Long

    /** Number of sessions completed at or after [since], e.g. the start of today. */
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completedAt >= :since")
    fun countSince(since: Long): LiveData<Int>

    @Query("SELECT * FROM focus_sessions ORDER BY completedAt")
    fun getAll(): LiveData<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions ORDER BY completedAt")
    suspend fun getAllOnce(): List<FocusSession>
}
