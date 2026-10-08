package dev.samadali.zen.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import dev.samadali.zen.ZenApp
import dev.samadali.zen.data.FocusSession
import dev.samadali.zen.data.Task
import java.time.LocalDate
import java.time.ZoneId

/**
 * Sessions and tasks combined into per-day activity and overall stats, shared by the
 * pomodoro streak, the calendar and the stats screen.
 */
class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<ZenApp>()

    private data class Records(val sessions: List<SessionRecord>, val tasks: List<TaskRecord>)

    private val records: LiveData<Records> = MediatorLiveData<Records>().apply {
        var sessions: List<FocusSession>? = null
        var tasks: List<Task>? = null
        fun update() {
            val s = sessions ?: return
            val t = tasks ?: return
            value = Records(
                s.map { SessionRecord(it.completedAt, it.durationMillis) },
                t.map { TaskRecord(it.name, it.completedAt) }
            )
        }
        addSource(app.database.focusSessionDao().getAll()) { sessions = it; update() }
        addSource(app.database.taskDao().getAll()) { tasks = it; update() }
    }

    val byDay: LiveData<Map<LocalDate, DayActivity>> = records.map {
        ActivityStats.byDay(it.sessions, it.tasks, ZoneId.systemDefault())
    }

    val summary: LiveData<StatsSummary> = records.map {
        ActivityStats.summary(it.sessions, it.tasks, app.joinedAt, LocalDate.now(), ZoneId.systemDefault())
    }
}
