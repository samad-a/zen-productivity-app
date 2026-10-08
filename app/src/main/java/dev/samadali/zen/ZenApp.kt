package dev.samadali.zen

import android.app.Application
import dev.samadali.zen.data.FocusSession
import dev.samadali.zen.data.ZenDatabase
import dev.samadali.zen.pomodoro.PomodoroTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZenApp : Application() {
    val database: ZenDatabase by lazy { ZenDatabase.create(this) }

    /** For work that must finish even if the screen that started it closes. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        PomodoroTimer.init(this)
        PomodoroTimer.onStudySessionCompleted = { startedAt, completedAt, duration ->
            applicationScope.launch {
                database.focusSessionDao().insert(
                    FocusSession(startedAt = startedAt, completedAt = completedAt, durationMillis = duration)
                )
            }
        }
    }
}
