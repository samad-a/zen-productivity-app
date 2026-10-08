package dev.samadali.zen.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import dev.samadali.zen.ui.UiTestSupport.eventually
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TasksScreenTest {
    @get:Rule(order = 0)
    val permission = UiTestSupport.notificationPermission()

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        UiTestSupport.resetAppState()
        onView(withId(R.id.tasks)).perform(click())
    }

    @Test
    fun addedTaskAppearsInTheList() {
        addTask("Revise maths", "Chapter 4")

        eventually { onView(withText("Revise maths")).check(matches(isDisplayed())) }
        onView(withText("Chapter 4")).check(matches(isDisplayed()))
    }

    @Test
    fun addingATaskWithoutANameShowsAnError() {
        onView(withId(R.id.addNewTaskButton)).perform(click())
        onView(withId(R.id.addTaskButton)).perform(click())

        onView(withId(R.id.taskNameInput))
            .check(matches(hasErrorText(UiTestSupport.context.getString(R.string.error_task_name_required))))
    }

    @Test
    fun tickingATaskMarksItDone() {
        addTask("Gym")
        eventually { onView(withText("Gym")).check(matches(isDisplayed())) }

        onView(checkBoxOf("Gym")).perform(click())

        eventually { onView(checkBoxOf("Gym")).check(matches(isChecked())) }
    }

    @Test
    fun swipingATaskDeletesItAndUndoBringsItBack() {
        addTask("Read")
        eventually { onView(withText("Read")).check(matches(isDisplayed())) }

        onView(withId(R.id.tasksRecyclerView))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeLeft()))
        eventually { onView(withText("Read")).check(doesNotExist()) }

        onView(withText(R.string.undo)).perform(click())
        eventually { onView(withText("Read")).check(matches(isDisplayed())) }
    }

    @Test
    fun tasksSurviveTheScreenBeingRecreated() {
        addTask("Write essay")
        eventually { onView(withText("Write essay")).check(matches(isDisplayed())) }

        activity.scenario.recreate()
        onView(withId(R.id.tasks)).perform(click())

        eventually { onView(withText("Write essay")).check(matches(isDisplayed())) }
    }

    private fun addTask(name: String, description: String = "") {
        onView(withId(R.id.addNewTaskButton)).perform(click())
        onView(withId(R.id.taskNameInput)).perform(typeText(name))
        if (description.isNotEmpty()) {
            onView(withId(R.id.taskDescriptionInput)).perform(typeText(description))
        }
        onView(withId(R.id.taskNameInput)).perform(closeSoftKeyboard())
        onView(withId(R.id.addTaskButton)).perform(click())
    }

    private fun checkBoxOf(taskName: String) =
        allOf(withId(R.id.checkBox), withParent(hasDescendant(withText(taskName))))
}
