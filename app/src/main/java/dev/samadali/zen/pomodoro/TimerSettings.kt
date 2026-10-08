package dev.samadali.zen.pomodoro

import android.content.SharedPreferences
import androidx.core.content.edit

/** The user's pomodoro preferences, stored in the timer's SharedPreferences. */
class TimerSettings(private val prefs: SharedPreferences) {

    var studyMinutes: Long
        get() = prefs.getLong(KEY_STUDY_MINUTES, DEFAULT_STUDY_MINUTES)
        set(value) = prefs.edit { putLong(KEY_STUDY_MINUTES, value) }

    var breakMinutes: Long
        get() = prefs.getLong(KEY_BREAK_MINUTES, DEFAULT_BREAK_MINUTES)
        set(value) = prefs.edit { putLong(KEY_BREAK_MINUTES, value) }

    var longBreakMinutes: Long
        get() = prefs.getLong(KEY_LONG_BREAK_MINUTES, DEFAULT_LONG_BREAK_MINUTES)
        set(value) = prefs.edit { putLong(KEY_LONG_BREAK_MINUTES, value) }

    /** Study sessions before a long break. */
    var sessionsPerLongBreak: Int
        get() = prefs.getInt(KEY_SESSIONS_PER_LONG_BREAK, DEFAULT_SESSIONS_PER_LONG_BREAK)
        set(value) = prefs.edit { putInt(KEY_SESSIONS_PER_LONG_BREAK, value) }

    /** Study sessions the user aims to finish each day. */
    var dailyGoal: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
        set(value) = prefs.edit { putInt(KEY_DAILY_GOAL, value) }

    var autoStartBreaks: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_BREAKS, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_START_BREAKS, value) }

    var autoStartStudy: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_STUDY, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_START_STUDY, value) }

    var sound: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit { putBoolean(KEY_SOUND, value) }

    var vibrate: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit { putBoolean(KEY_VIBRATE, value) }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = prefs.edit { putBoolean(KEY_KEEP_SCREEN_ON, value) }

    companion object {
        const val PREFS_NAME = "pomodoro"

        const val DEFAULT_STUDY_MINUTES = 25L
        const val DEFAULT_BREAK_MINUTES = 5L
        const val DEFAULT_LONG_BREAK_MINUTES = 15L
        const val DEFAULT_SESSIONS_PER_LONG_BREAK = 4
        const val DEFAULT_DAILY_GOAL = 8

        private const val KEY_STUDY_MINUTES = "study_minutes"
        private const val KEY_BREAK_MINUTES = "break_minutes"
        private const val KEY_LONG_BREAK_MINUTES = "long_break_minutes"
        private const val KEY_SESSIONS_PER_LONG_BREAK = "sessions_per_long_break"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_AUTO_START_BREAKS = "auto_start_breaks"
        private const val KEY_AUTO_START_STUDY = "auto_start_study"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    }
}
