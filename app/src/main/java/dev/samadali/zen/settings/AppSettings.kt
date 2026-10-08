package dev.samadali.zen.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

/** App-wide preferences: theme and which optional notifications the user wants. */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    enum class Theme(val nightMode: Int) {
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES)
    }

    var theme: Theme
        get() = Theme.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: Theme.SYSTEM
        set(value) {
            prefs.edit { putString(KEY_THEME, value.name) }
            AppCompatDelegate.setDefaultNightMode(value.nightMode)
        }

    var taskReminders: Boolean
        get() = prefs.getBoolean(KEY_TASK_REMINDERS, true)
        set(value) = prefs.edit { putBoolean(KEY_TASK_REMINDERS, value) }

    /** An evening nudge when today's session is still needed to keep the streak. */
    var streakReminders: Boolean
        get() = prefs.getBoolean(KEY_STREAK_REMINDERS, false)
        set(value) = prefs.edit { putBoolean(KEY_STREAK_REMINDERS, value) }

    /** A short morning tip to get started. */
    var studyMotivation: Boolean
        get() = prefs.getBoolean(KEY_STUDY_MOTIVATION, false)
        set(value) = prefs.edit { putBoolean(KEY_STUDY_MOTIVATION, value) }

    companion object {
        /** Shared with ZenApp, which keeps the join date here. */
        const val PREFS_NAME = "app"
        private const val KEY_THEME = "theme"
        private const val KEY_TASK_REMINDERS = "task_reminders"
        private const val KEY_STREAK_REMINDERS = "streak_reminders"
        private const val KEY_STUDY_MOTIVATION = "study_motivation"
    }
}
