package dev.samadali.zen.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import dev.samadali.zen.ui.UiTestSupport.addSession
import dev.samadali.zen.ui.UiTestSupport.eventually
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The streak, calendar and stats, which all read the logged study sessions. */
@RunWith(AndroidJUnit4::class)
class ProgressScreensTest {
    @get:Rule(order = 0)
    val permission = UiTestSupport.notificationPermission()

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() = UiTestSupport.resetAppState()

    @Test
    fun streakCountsConsecutiveStudyDays() {
        addSession(daysAgo = 0)
        addSession(daysAgo = 1)
        addSession(daysAgo = 2)
        addSession(daysAgo = 4)

        eventually { onView(withId(R.id.streakText)).check(matches(withText("3 day streak"))) }
    }

    @Test
    fun streakWarnsWhenTodayHasNoSessionYet() {
        addSession(daysAgo = 1)

        eventually {
            onView(withId(R.id.streakText)).check(matches(withText("1 day streak: study today to keep it!")))
        }
    }

    @Test
    fun calendarShowsTheSelectedDaysSessions() {
        addSession(daysAgo = 0)
        addSession(daysAgo = 0)

        onView(withId(R.id.calendar)).perform(click())

        eventually { onView(withId(R.id.daySessionsText)).check(matches(withText("2 study sessions"))) }
        onView(withId(R.id.dayFocusText)).check(matches(withText("50 min focused")))
        val today = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        onView(withContentDescription("$today, 2 study sessions")).check(matches(isDisplayed()))
    }

    @Test
    fun calendarCanGoBackAMonthAndNotPastThisMonth() {
        onView(withId(R.id.calendar)).perform(click())
        val thisMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("LLLL yyyy"))
        val lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("LLLL yyyy"))

        onView(withId(R.id.previousMonthButton)).perform(click())
        onView(withId(R.id.monthText)).check(matches(withText(lastMonth)))

        onView(withId(R.id.nextMonthButton)).perform(click())
        onView(withId(R.id.monthText)).check(matches(withText(thisMonth)))
        onView(withId(R.id.nextMonthButton)).check(matches(isNotEnabled()))
    }

    @Test
    fun statsSummariseSessions() {
        addSession(daysAgo = 0)
        addSession(daysAgo = 1)
        addSession(daysAgo = 3)

        onView(withId(R.id.stats)).perform(click())

        eventually {
            onView(allOf(withId(R.id.statContent), hasDescendant(withText("hours focused"))))
                .check(matches(withContentDescription("1.3 hours focused")))
        }
        onView(allOf(withId(R.id.statContent), hasDescendant(withText("days longest streak"))))
            .check(matches(withContentDescription("2 days longest streak")))
        onView(withId(R.id.yearHeatmap))
            .check(matches(withContentDescription(startsWith("Study sessions over the last year: 3 study days"))))
    }
}
