package dev.samadali.zen.pomodoro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import dev.samadali.zen.ZenApp
import java.util.Calendar

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = getApplication<ZenApp>().database.focusSessionDao()

    /** Study sessions completed since midnight, for the daily goal. */
    val sessionsToday: LiveData<Int> = dao.countSince(startOfToday())

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
