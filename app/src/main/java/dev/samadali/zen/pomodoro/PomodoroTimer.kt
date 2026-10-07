package dev.samadali.zen.pomodoro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

    var studyMinutes = 25L
        private set
    var breakMinutes = 5L
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
        studyMinutes = prefs.getLong("study_minutes", 25L)
        breakMinutes = prefs.getLong("break_minutes", 5L)

        val phase = runCatching { Phase.valueOf(prefs.getString("phase", null)!!) }
            .getOrDefault(Phase.STUDY)
        val total = durationOf(phase)
        val remaining = prefs.getLong("remaining", total)
        _phase.value = phase
        _totalMillis.value = total
        _remainingMillis.value = if (remaining in 1..total) remaining else total
    }

    fun start(context: Context) {
        if (_isRunning.value == true) return
        endAtElapsed = SystemClock.elapsedRealtime() + (_remainingMillis.value ?: 0L)
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

    fun setStudyMinutes(minutes: Long) {
        studyMinutes = minutes
        prefs.edit { putLong("study_minutes", minutes) }
        if (_phase.value == Phase.STUDY && _isRunning.value != true) resetPhase()
    }

    fun setBreakMinutes(minutes: Long) {
        breakMinutes = minutes
        prefs.edit { putLong("break_minutes", minutes) }
        if (_phase.value == Phase.BREAK && _isRunning.value != true) resetPhase()
    }

    /** Wall-clock time the current phase will end, for the notification countdown. */
    fun phaseEndsAtWallClock(): Long = System.currentTimeMillis() + remainingNow()

    fun remainingNow(): Long =
        if (_isRunning.value == true) maxOf(0L, endAtElapsed - SystemClock.elapsedRealtime())
        else _remainingMillis.value ?: 0L

    private fun finishPhase() {
        val finished = _phase.value ?: Phase.STUDY
        val next = if (finished == Phase.STUDY) Phase.BREAK else Phase.STUDY
        val duration = durationOf(next)
        endAtElapsed = SystemClock.elapsedRealtime() + duration
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
            putString("phase", _phase.value?.name)
            putLong("remaining", _remainingMillis.value ?: 0L)
        }
    }

    private fun durationOf(phase: Phase): Long =
        (if (phase == Phase.STUDY) studyMinutes else breakMinutes) * 60 * 1000
}
