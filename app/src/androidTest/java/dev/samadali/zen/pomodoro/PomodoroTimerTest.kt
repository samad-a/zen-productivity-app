package dev.samadali.zen.pomodoro

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Covers the timer's persisted settings; starting the timer needs the foreground service. */
@RunWith(AndroidJUnit4::class)
class PomodoroTimerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        clearPrefs()
        onMain { PomodoroTimer.init(context) }
    }

    @After
    fun tearDown() {
        clearPrefs()
        onMain { PomodoroTimer.init(context) }
    }

    @Test
    fun init_usesDefaultDurations() = onMain {
        assertEquals(25L, PomodoroTimer.studyMinutes)
        assertEquals(5L, PomodoroTimer.breakMinutes)
        assertEquals(Phase.STUDY, PomodoroTimer.phase.value)
        assertEquals(25 * 60 * 1000L, PomodoroTimer.remainingMillis.current)
    }

    @Test
    fun setStudyMinutes_resetsTheIdleStudyPhase() = onMain {
        PomodoroTimer.setStudyMinutes(40)

        assertEquals(40 * 60 * 1000L, PomodoroTimer.totalMillis.current)
        assertEquals(40 * 60 * 1000L, PomodoroTimer.remainingMillis.current)
    }

    @Test
    fun setBreakMinutes_doesNotTouchTheStudyPhase() = onMain {
        PomodoroTimer.setBreakMinutes(10)

        assertEquals(10L, PomodoroTimer.breakMinutes)
        assertEquals(25 * 60 * 1000L, PomodoroTimer.remainingMillis.current)
    }

    @Test
    fun durations_surviveReinitialisation() = onMain {
        PomodoroTimer.setStudyMinutes(50)
        PomodoroTimer.setBreakMinutes(15)

        PomodoroTimer.init(context)

        assertEquals(50L, PomodoroTimer.studyMinutes)
        assertEquals(15L, PomodoroTimer.breakMinutes)
        assertEquals(50 * 60 * 1000L, PomodoroTimer.remainingMillis.current)
    }

    private val LiveData<Long>.current: Long
        get() = checkNotNull(value) { "LiveData has no value" }

    private fun clearPrefs() {
        context.getSharedPreferences("pomodoro", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // PomodoroTimer's LiveData must be updated on the main thread
    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
