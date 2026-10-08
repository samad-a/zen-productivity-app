package dev.samadali.zen.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule(order = 0)
    val permission = UiTestSupport.notificationPermission()

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() = UiTestSupport.resetAppState()

    @Test
    fun opensStraightIntoThePomodoroScreen() {
        onView(withId(R.id.pomodoroTitleText)).check(matches(isDisplayed()))
    }

    @Test
    fun eachTabShowsItsScreen() {
        onView(withId(R.id.tasks)).perform(click())
        onView(withId(R.id.tasksTitle)).check(matches(isDisplayed()))

        onView(withId(R.id.calendar)).perform(click())
        onView(withId(R.id.calendarTitle)).check(matches(isDisplayed()))

        onView(withId(R.id.stats)).perform(click())
        onView(withId(R.id.statsTitle)).check(matches(isDisplayed()))

        onView(withId(R.id.settings)).perform(click())
        onView(withId(R.id.settingsTitle)).check(matches(isDisplayed()))

        onView(withId(R.id.pomodoro)).perform(click())
        onView(withId(R.id.pomodoroTitleText)).check(matches(isDisplayed()))
    }

    @Test
    fun backFromAddTaskReturnsToTheTaskList() {
        onView(withId(R.id.tasks)).perform(click())
        onView(withId(R.id.addNewTaskButton)).perform(click())
        onView(withId(R.id.taskNameInput)).check(matches(isDisplayed()))

        pressBack()

        onView(withId(R.id.tasksTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun switchingTabsFromAddTaskDropsIt() {
        onView(withId(R.id.tasks)).perform(click())
        onView(withId(R.id.addNewTaskButton)).perform(click())
        onView(withId(R.id.settings)).perform(click())
        onView(withId(R.id.tasks)).perform(click())

        onView(withId(R.id.tasksTitle)).check(matches(isDisplayed()))
    }
}
