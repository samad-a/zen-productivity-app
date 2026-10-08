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

enum class Phase { STUDY, BREAK }

/**
 * App-wide pomodoro state. The countdown is driven by an end timestamp rather than
 * accumulated ticks, so the remaining time stays correct even if ticks are delayed.
 * [PomodoroService] keeps the process alive while the timer is running.
 */
object PomodoroTimer {
    private const val TICK_MS = 100L
    private const val DEFAULT_STUDY_MINUTES = 25L
    private const val DEFAULT_BREAK_MINUTES = 5L
    private const val KEY_STUDY_MINUTES = "study_minutes"
    private const val KEY_BREAK_MINUTES = "break_minutes"
    private const val KEY_PHASE = "phase"
    private const val KEY_REMAINING = "remaining"

    /** Monotonic time in milliseconds. Tests replace it to skip through phases instantly. */
    @VisibleForTesting
    var clock: () -> Long = SystemClock::elapsedRealtime

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private var endAtElapsed = 0L

    private val _phase = MutableLiveData(Phase.STUDY)
    val phase: LiveData<Phase> = _phase

    private val _totalMillis = MutableLiveData(0L)
    val totalMillis: LiveData<Long> = _totalMillis

    private val _remainingMillis = MutableLiveData(0L)
    val remainingMillis: LiveData<Long> = _remainingMillis

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    var studyMinutes = DEFAULT_STUDY_MINUTES
        private set
    var breakMinutes = DEFAULT_BREAK_MINUTES
        private set

    /** Called on the main thread when a phase runs out, with the phase that just ended. */
    var onPhaseFinished: ((Phase) -> Unit)? = null

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
        prefs = context.getSharedPreferences("pomodoro", Context.MODE_PRIVATE)
        studyMinutes = prefs.getLong(KEY_STUDY_MINUTES, DEFAULT_STUDY_MINUTES)
        breakMinutes = prefs.getLong(KEY_BREAK_MINUTES, DEFAULT_BREAK_MINUTES)

        val phase = runCatching { Phase.valueOf(prefs.getString(KEY_PHASE, null)!!) }
            .getOrDefault(Phase.STUDY)
        val total = durationOf(phase)
        val remaining = prefs.getLong(KEY_REMAINING, total)
        _phase.value = phase
        _totalMillis.value = total
        _remainingMillis.value = if (remaining in 1..total) remaining else total
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

    // Unchanged values are ignored: the duration fields re-send their text when the
    // screen is recreated, which would otherwise reset a paused phase.
    fun setStudyMinutes(minutes: Long) {
        if (minutes == studyMinutes) return
        studyMinutes = minutes
        prefs.edit { putLong(KEY_STUDY_MINUTES, minutes) }
        if (_phase.value == Phase.STUDY && _isRunning.value != true) resetPhase()
    }

    fun setBreakMinutes(minutes: Long) {
        if (minutes == breakMinutes) return
        breakMinutes = minutes
        prefs.edit { putLong(KEY_BREAK_MINUTES, minutes) }
        if (_phase.value == Phase.BREAK && _isRunning.value != true) resetPhase()
    }

    /** Wall-clock time the current phase will end, for the notification countdown. */
    fun phaseEndsAtWallClock(): Long = System.currentTimeMillis() + remainingNow()

    fun remainingNow(): Long =
        if (_isRunning.value == true) maxOf(0L, endAtElapsed - clock())
        else _remainingMillis.value ?: 0L

    private fun finishPhase() {
        val finished = _phase.value ?: Phase.STUDY
        val next = if (finished == Phase.STUDY) Phase.BREAK else Phase.STUDY
        val duration = durationOf(next)
        endAtElapsed = clock() + duration
        _phase.value = next
        _totalMillis.value = duration
        _remainingMillis.value = duration
        onPhaseFinished?.invoke(finished)
        handler.post(tick)
    }

    private fun resetPhase() {
        val duration = durationOf(_phase.value ?: Phase.STUDY)
        _totalMillis.value = duration
        _remainingMillis.value = duration
        saveProgress()
    }

    private fun saveProgress() {
        prefs.edit {
            putString(KEY_PHASE, _phase.value?.name)
            putLong(KEY_REMAINING, _remainingMillis.value ?: 0L)
        }
    }

    private fun durationOf(phase: Phase): Long =
        (if (phase == Phase.STUDY) studyMinutes else breakMinutes) * 60 * 1000
}
