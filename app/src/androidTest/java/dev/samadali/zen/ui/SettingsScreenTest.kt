package dev.samadali.zen.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import dev.samadali.zen.ZenApp
import dev.samadali.zen.data.Task
import dev.samadali.zen.settings.AppSettings
import dev.samadali.zen.ui.UiTestSupport.addSession
import dev.samadali.zen.ui.UiTestSupport.eventually
import dev.samadali.zen.ui.UiTestSupport.onMain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule(order = 0)
    val permission = UiTestSupport.notificationPermission()

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    private val app get() = UiTestSupport.context as ZenApp

    @Before
    fun setUp() {
        UiTestSupport.resetAppState()
        onView(withId(R.id.settings)).perform(click())
    }

    @After
    fun tearDown() {
        onMain { AppSettings(app).theme = AppSettings.Theme.SYSTEM }
    }

    @Test
    fun timerSettingsOpenFromSettings() {
        onView(row(R.id.timerRow)).perform(scrollTo(), click())

        onView(withId(R.id.studyLengthSlider)).check(matches(isDisplayed()))
    }

    @Test
    fun choosingDarkSwitchesTheTheme() {
        onView(row(R.id.themeRow)).perform(scrollTo(), click())
        onView(withText(R.string.theme_dark)).perform(click())

        eventually { assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode()) }
        onView(withId(R.id.settingsTitle)).check(matches(isDisplayed()))
        assertEquals(AppSettings.Theme.DARK, AppSettings(app).theme)
    }

    @Test
    fun deleteAllDataAsksFirstThenClearsEverything() {
        runBlocking { app.database.taskDao().insert(Task(name = "Gym", description = "")) }
        addSession(daysAgo = 0)

        onView(row(R.id.deleteRow)).perform(scrollTo(), click())
        onView(withText(R.string.delete)).perform(click())

        eventually {
            runBlocking {
                assertTrue(app.database.taskDao().getAllOnce().isEmpty())
                assertTrue(app.database.focusSessionDao().getAllOnce().isEmpty())
            }
        }
    }

    @Test
    fun privacyExplainsThatDataStaysOnTheDevice() {
        onView(row(R.id.privacyRow)).perform(scrollTo(), click())

        onView(withText(R.string.privacy_text)).check(matches(isDisplayed()))
    }

    /** A settings row; the include's id replaces the row layout's own root id. */
    private fun row(includeId: Int) = withId(includeId)
}
