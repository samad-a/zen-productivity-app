package dev.samadali.zen

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import dev.samadali.zen.data.FocusSession
import dev.samadali.zen.data.ZenDatabase
import dev.samadali.zen.pomodoro.PomodoroTimer
import dev.samadali.zen.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZenApp : Application() {
    val database: ZenDatabase by lazy { ZenDatabase.create(this) }

    /** For work that must finish even if the screen that started it closes. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * When the user started using the app, for "days since joining". Stored rather than read
     * from the install time so it survives reinstalling and is restored from backup.
     */
    val joinedAt: Long by lazy {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.getLong(KEY_JOINED_AT, 0L).takeIf { it > 0 } ?: run {
            val installedAt = packageManager.getPackageInfo(packageName, 0).firstInstallTime
            prefs.edit { putLong(KEY_JOINED_AT, installedAt) }
            installedAt
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppSettings(this).theme.nightMode)
        joinedAt
        PomodoroTimer.init(this)
        PomodoroTimer.onStudySessionCompleted = { startedAt, completedAt, duration ->
            applicationScope.launch {
                database.focusSessionDao().insert(
                    FocusSession(startedAt = startedAt, completedAt = completedAt, durationMillis = duration)
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME = AppSettings.PREFS_NAME
        private const val KEY_JOINED_AT = "joined_at"
    }
}
