package dev.samadali.zen.pomodoro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

enum class Phase {
    STUDY, BREAK, LONG_BREAK;

    val isBreak: Boolean get() = this != STUDY
}

/**
 * App-wide pomodoro state. The countdown is driven by an end timestamp rather than
 * accumulated ticks, so the remaining time stays correct even if ticks are delayed.
 * [PomodoroService] keeps the process alive while the timer is running.
 *
 * A cycle is [TimerSettings.sessionsPerLongBreak] study sessions with short breaks between
 * them, followed by a long break.
 */
object PomodoroTimer {
    private const val TICK_MS = 100L
    private const val KEY_PHASE = "phase"
    private const val KEY_REMAINING = "remaining"
    private const val KEY_COMPLETED_IN_CYCLE = "completed_in_cycle"

    /** Monotonic time in milliseconds. Tests replace it to skip through phases instantly. */
    @VisibleForTesting
    var clock: () -> Long = SystemClock::elapsedRealtime

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    lateinit var settings: TimerSettings
        private set
    private var endAtElapsed = 0L

    private val _phase = MutableLiveData(Phase.STUDY)
    val phase: LiveData<Phase> = _phase

    private val _totalMillis = MutableLiveData(0L)
    val totalMillis: LiveData<Long> = _totalMillis

    private val _remainingMillis = MutableLiveData(0L)
    val remainingMillis: LiveData<Long> = _remainingMillis

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _completedInCycle = MutableLiveData(0)
    /** Study sessions finished since the last long break. */
    val completedInCycle: LiveData<Int> = _completedInCycle

    val studyMinutes: Long get() = settings.studyMinutes
    val breakMinutes: Long get() = settings.breakMinutes

    /** Called on the main thread when a phase runs out, with the phase that just ended. */
    var onPhaseFinished: ((Phase) -> Unit)? = null

    /** Called on the main thread when a study session runs to the end, to log it. */
    var onStudySessionCompleted: ((startedAt: Long, completedAt: Long, durationMillis: Long) -> Unit)? = null

    private val tick = object : Runnable {
        override fun run() {
            val left = remainingNow()
            if (left > 0) {
                _remainingMillis.value = left
                handler.postDelayed(this, TICK_MS)
            } else {
                finishPhase()
            }
        }
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(TimerSettings.PREFS_NAME, Context.MODE_PRIVATE)
        settings = TimerSettings(prefs)

        val phase = runCatching { Phase.valueOf(prefs.getString(KEY_PHASE, null)!!) }
            .getOrDefault(Phase.STUDY)
        val total = durationOf(phase)
        val remaining = prefs.getLong(KEY_REMAINING, total)
        _phase.value = phase
        _totalMillis.value = total
        _remainingMillis.value = if (remaining in 1..total) remaining else total
        _completedInCycle.value = prefs.getInt(KEY_COMPLETED_IN_CYCLE, 0)
    }

    fun start(context: Context) {
        if (_isRunning.value == true) return
        endAtElapsed = clock() + (_remainingMillis.value ?: 0L)
        _isRunning.value = true
        handler.post(tick)
        ContextCompat.startForegroundService(context, Intent(context, PomodoroService::class.java))
    }

    fun pause() {
        if (_isRunning.value != true) return
        handler.removeCallbacks(tick)
        _remainingMillis.value = remainingNow()
        _isRunning.value = false
        saveProgress()
    }

    /** Stops the timer and puts the current phase back to its full length. */
    fun reset() {
        pause()
        resetPhase()
    }

    /**
     * Moves straight to the next phase without logging the current one. Skipping a study
     * session doesn't count towards the long break. The next phase starts if the timer was
     * running and auto-start is on for it.
     */
    fun skip(context: Context) {
        val wasRunning = _isRunning.value == true
        pause()
        val next = if (_phase.value == Phase.STUDY) Phase.BREAK else Phase.STUDY
        if (_phase.value == Phase.LONG_BREAK) _completedInCycle.value = 0
        enterPhase(next)
        if (wasRunning && autoStarts(next)) start(context)
    }

    fun setStudyMinutes(minutes: Long) {
        // Unchanged values are ignored: the duration fields re-send their text when the
        // screen is recreated, which would otherwise reset a paused phase.
        if (minutes == settings.studyMinutes) return
        settings.studyMinutes = minutes
        if (_phase.value == Phase.STUDY && _isRunning.value != true) resetPhase()
    }

    fun setBreakMinutes(minutes: Long) {
        if (minutes == settings.breakMinutes) return
        settings.breakMinutes = minutes
        if (_phase.value == Phase.BREAK && _isRunning.value != true) resetPhase()
    }

    fun setLongBreakMinutes(minutes: Long) {
        if (minutes == settings.longBreakMinutes) return
        settings.longBreakMinutes = minutes
        if (_phase.value == Phase.LONG_BREAK && _isRunning.value != true) resetPhase()
    }

    /** Wall-clock time the current phase will end, for the notification countdown. */
    fun phaseEndsAtWallClock(): Long = System.currentTimeMillis() + remainingNow()

    fun remainingNow(): Long =
        if (_isRunning.value == true) maxOf(0L, endAtElapsed - clock())
        else _remainingMillis.value ?: 0L

    private fun finishPhase() {
        val finished = _phase.value ?: Phase.STUDY
        val next = when (finished) {
            Phase.STUDY -> {
                val total = _totalMillis.value ?: durationOf(Phase.STUDY)
                val now = System.currentTimeMillis()
                onStudySessionCompleted?.invoke(now - total, now, total)
                val completed = (_completedInCycle.value ?: 0) + 1
                _completedInCycle.value = completed
                if (completed >= settings.sessionsPerLongBreak) Phase.LONG_BREAK else Phase.BREAK
            }
            Phase.BREAK -> Phase.STUDY
            Phase.LONG_BREAK -> {
                _completedInCycle.value = 0
                Phase.STUDY
            }
        }
        // The next phase is timed from when this one ended, not from when the tick ran
        val endedAt = endAtElapsed
        enterPhase(next)
        // Alert before stopping, since the service stops itself once the timer isn't running
        onPhaseFinished?.invoke(finished)
        if (autoStarts(next)) {
            endAtElapsed = endedAt + durationOf(next)
            handler.post(tick)
        } else {
            _isRunning.value = false
        }
        saveProgress()
    }

    private fun enterPhase(phase: Phase) {
        val duration = durationOf(phase)
        _phase.value = phase
        _totalMillis.value = duration
        _remainingMillis.value = duration
        saveProgress()
    }

    private fun autoStarts(phase: Phase): Boolean =
        if (phase.isBreak) settings.autoStartBreaks else settings.autoStartStudy

    private fun resetPhase() {
        enterPhase(_phase.value ?: Phase.STUDY)
    }

    private fun saveProgress() {
        prefs.edit {
            putString(KEY_PHASE, _phase.value?.name)
            putLong(KEY_REMAINING, _remainingMillis.value ?: 0L)
            putInt(KEY_COMPLETED_IN_CYCLE, _completedInCycle.value ?: 0)
        }
    }

    private fun durationOf(phase: Phase): Long = when (phase) {
        Phase.STUDY -> settings.studyMinutes
        Phase.BREAK -> settings.breakMinutes
        Phase.LONG_BREAK -> settings.longBreakMinutes
    } * 60 * 1000
}
