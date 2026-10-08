package dev.samadali.zen.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import dev.samadali.zen.pomodoro.Phase
import dev.samadali.zen.pomodoro.PomodoroTimer
import dev.samadali.zen.ui.UiTestSupport.eventually
import dev.samadali.zen.ui.UiTestSupport.onMain
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PomodoroScreenTest {
    @get:Rule(order = 0)
    val permission = UiTestSupport.notificationPermission()

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    /** A fake clock the tests move forward by hand, so no test waits real minutes. */
    private var now = 1_000_000L

    @Before
    fun setUp() {
        UiTestSupport.resetAppState()
        onMain { PomodoroTimer.clock = { now } }
    }

    @After
    fun tearDown() = UiTestSupport.resetAppState()

    @Test
    fun showsTheStudyDurationBeforeStarting() {
        onView(withId(R.id.clockTimer)).check(matches(withText("25:00")))
        onView(withId(R.id.startStopButton)).check(matches(withText(R.string.start)))
        onView(withId(R.id.phaseText)).check(matches(withText("Session 1 of 4")))
        onView(withId(R.id.todayText)).check(matches(withText("0 of 8 sessions today")))
    }

    @Test
    fun startingLocksTheDurationsAndCountsDown() {
        onView(withId(R.id.startStopButton)).perform(click())

        onView(withId(R.id.startStopButton)).check(matches(withText(R.string.pause)))
        onView(withId(R.id.studyTextInput)).check(matches(not(isEnabled())))
        onView(withId(R.id.breakTextInput)).check(matches(not(isEnabled())))

        now += 60_000
        eventually { onView(withId(R.id.clockTimer)).check(matches(withText("24:00"))) }
    }

    @Test
    fun pausingKeepsTheRemainingTime() {
        onView(withId(R.id.startStopButton)).perform(click())
        now += 90_000
        eventually { onView(withId(R.id.clockTimer)).check(matches(withText("23:30"))) }

        onView(withId(R.id.startStopButton)).perform(click())
        now += 10 * 60_000

        onView(withId(R.id.startStopButton)).check(matches(withText(R.string.start)))
        onView(withId(R.id.clockTimer)).check(matches(withText("23:30")))
        onView(withId(R.id.studyTextInput)).check(matches(isEnabled()))
    }

    @Test
    fun finishingTheStudyPhaseStartsTheBreak() {
        onView(withId(R.id.startStopButton)).perform(click())

        now += 25 * 60_000

        eventually { onView(withId(R.id.clockTimer)).check(matches(withText("05:00"))) }
        onMain { assertEquals(Phase.BREAK, PomodoroTimer.phase.value) }
    }

    @Test
    fun changingTheStudyDurationUpdatesTheTimer() {
        onView(withId(R.id.studyTextInput)).perform(replaceText("40"), closeSoftKeyboard())

        onView(withId(R.id.clockTimer)).check(matches(withText("40:00")))
    }

    @Test
    fun aPausedTimerSurvivesTheScreenBeingRecreated() {
        onView(withId(R.id.startStopButton)).perform(click())
        now += 5 * 60_000
        eventually { onView(withId(R.id.clockTimer)).check(matches(withText("20:00"))) }
        onView(withId(R.id.startStopButton)).perform(click())

        activity.scenario.recreate()

        onView(withId(R.id.clockTimer)).check(matches(withText("20:00")))
    }

    @Test
    fun completingAStudySessionCountsTowardsToday() {
        onView(withId(R.id.startStopButton)).perform(click())

        now += 25 * 60_000

        eventually { onView(withId(R.id.todayText)).check(matches(withText("1 of 8 sessions today"))) }
        onView(withId(R.id.phaseText)).check(matches(withText(R.string.short_break)))
    }

    @Test
    fun theFourthStudySessionIsFollowedByALongBreak() {
        onView(withId(R.id.startStopButton)).perform(click())

        // Four study sessions with three short breaks between them
        now += (4 * 25 + 3 * 5) * 60_000L

        eventually { onView(withId(R.id.phaseText)).check(matches(withText(R.string.long_break))) }
        onView(withId(R.id.clockTimer)).check(matches(withText("15:00")))
        eventually { onView(withId(R.id.todayText)).check(matches(withText("4 of 8 sessions today"))) }

        now += 15 * 60_000
        eventually { onView(withId(R.id.phaseText)).check(matches(withText("Session 1 of 4"))) }
    }

    @Test
    fun withAutoStartOffTheBreakWaitsForTheUser() {
        onMain { PomodoroTimer.settings.autoStartBreaks = false }
        onView(withId(R.id.startStopButton)).perform(click())

        now += 25 * 60_000

        eventually { onView(withId(R.id.startStopButton)).check(matches(withText(R.string.start))) }
        onView(withId(R.id.clockTimer)).check(matches(withText("05:00")))
        onView(withId(R.id.phaseText)).check(matches(withText(R.string.short_break)))
    }

    @Test
    fun resetPutsThePhaseBackToTheStart() {
        onView(withId(R.id.startStopButton)).perform(click())
        now += 5 * 60_000
        eventually { onView(withId(R.id.clockTimer)).check(matches(withText("20:00"))) }

        onView(withId(R.id.resetButton)).perform(click())

        onView(withId(R.id.clockTimer)).check(matches(withText("25:00")))
        onView(withId(R.id.startStopButton)).check(matches(withText(R.string.start)))
    }

    @Test
    fun skipMovesToTheNextPhaseWithoutCountingTheSession() {
        onView(withId(R.id.skipButton)).perform(click())

        onView(withId(R.id.phaseText)).check(matches(withText(R.string.short_break)))
        onView(withId(R.id.clockTimer)).check(matches(withText("05:00")))

        onView(withId(R.id.skipButton)).perform(click())

        onView(withId(R.id.phaseText)).check(matches(withText("Session 1 of 4")))
        onView(withId(R.id.todayText)).check(matches(withText("0 of 8 sessions today")))
    }

    @Test
    fun timerSettingsSheetOpens() {
        onView(withId(R.id.timerSettingsButton)).perform(scrollTo(), click())

        onView(withId(R.id.longBreakSlider)).check(matches(isDisplayed()))
    }
}
