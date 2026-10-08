package dev.samadali.zen.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import dev.samadali.zen.ZenApp
import dev.samadali.zen.data.TaskCategory
import dev.samadali.zen.ui.UiTestSupport.eventually
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
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
    fun emptyListExplainsHowToStart() {
        eventually { onView(withId(R.id.emptyText)).check(matches(isDisplayed())) }

        addTask("Gym")

        eventually { onView(withId(R.id.emptyText)).check(matches(withEffectiveVisibility(Visibility.GONE))) }
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
        onView(withId(R.id.addTaskButton)).perform(scrollTo(), click())

        onView(withId(R.id.taskNameInput))
            .check(matches(hasErrorText(UiTestSupport.context.getString(R.string.error_task_name_required))))
    }

    @Test
    fun theChosenCategoryIsSaved() {
        onView(withId(R.id.addNewTaskButton)).perform(click())
        onView(withId(R.id.taskNameInput)).perform(replaceText("Leg day"))
        onView(withText(R.string.category_gym)).perform(click())
        onView(withId(R.id.addTaskButton)).perform(scrollTo(), click())

        eventually { onView(withText("Leg day")).check(matches(isDisplayed())) }
        val saved = (UiTestSupport.context as ZenApp).database.query("SELECT category FROM tasks", null).use {
            it.moveToFirst()
            it.getString(0)
        }
        assertEquals(TaskCategory.GYM.name, saved)
    }

    @Test
    fun tappingATaskOpensItForEditing() {
        addTask("Reed")
        eventually { onView(withText("Reed")).check(matches(isDisplayed())) }

        onView(withText("Reed")).perform(click())
        onView(withId(R.id.editorTitle)).check(matches(withText(R.string.edit_task)))
        onView(withId(R.id.taskNameInput)).perform(replaceText("Read"))
        onView(withId(R.id.addTaskButton)).perform(scrollTo(), click())

        eventually { onView(withText("Read")).check(matches(isDisplayed())) }
        onView(withText("Reed")).check(doesNotExist())
    }

    @Test
    fun finishedTasksMoveToACollapsedSection() {
        addTask("Gym")
        eventually { onView(withText("Gym")).check(matches(isDisplayed())) }

        onView(checkBoxOf("Gym")).perform(click())

        eventually { onView(withText("Completed (1)")).check(matches(isDisplayed())) }
        onView(withText("Gym")).check(doesNotExist())

        onView(withText("Completed (1)")).perform(click())
        eventually { onView(checkBoxOf("Gym")).check(matches(isChecked())) }
    }

    @Test
    fun clearCompletedRemovesFinishedTasksWithUndo() {
        addTask("Laundry")
        addTask("Gym")
        eventually { onView(withText("Laundry")).check(matches(isDisplayed())) }
        onView(checkBoxOf("Laundry")).perform(click())
        eventually { onView(withText("Completed (1)")).check(matches(isDisplayed())) }

        onView(withId(R.id.clearCompletedButton)).perform(click())

        eventually { onView(withText("Completed (1)")).check(doesNotExist()) }
        onView(withText("Gym")).check(matches(isDisplayed()))

        onView(withText(R.string.undo)).perform(click())
        eventually { onView(withText("Completed (1)")).check(matches(isDisplayed())) }
    }

    @Test
    fun tasksCanBeReorderedWithoutDragging() {
        addTask("First")
        addTask("Second")
        eventually { onView(withText("Second")).check(matches(isDisplayed())) }

        // The "Move down" action TalkBack offers on each task
        onView(cardOf("First")).perform(accessibilityAction(R.id.action_move_down))

        eventually { onView(atPosition(0)).check(matches(hasDescendant(withText("Second")))) }
        onView(atPosition(1)).check(matches(hasDescendant(withText("First"))))
    }

    @Test
    fun swipingATaskDeletesItAndUndoBringsItBack() {
        addTask("Read")
        eventually { onView(withText("Read")).check(matches(isDisplayed())) }

        // From the left: a touch that starts on the drag handle at the right edge drags instead
        onView(withId(R.id.tasksRecyclerView))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeRight()))
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

    // replaceText rather than typeText: the emulator keyboard sometimes drops characters
    private fun addTask(name: String, description: String = "") {
        onView(withId(R.id.addNewTaskButton)).perform(click())
        onView(withId(R.id.taskNameInput)).perform(replaceText(name))
        if (description.isNotEmpty()) {
            onView(withId(R.id.taskDescriptionInput)).perform(replaceText(description))
        }
        onView(withId(R.id.addTaskButton)).perform(scrollTo(), click())
    }

    private fun checkBoxOf(taskName: String) =
        allOf(withId(R.id.checkBox), withParent(hasDescendant(withText(taskName))))

    private fun cardOf(taskName: String) = allOf(withId(R.id.taskCard), hasDescendant(withText(taskName)))

    /** The task card at [position] in the list. */
    private fun atPosition(position: Int): Matcher<View> =
        allOf(withId(R.id.taskCard), withParent(withId(R.id.tasksRecyclerView)), PositionMatcher(position))

    private class PositionMatcher(private val position: Int) : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("is at adapter position $position")
        }

        override fun matchesSafely(item: View): Boolean {
            val parent = item.parent as? RecyclerView ?: return false
            return parent.getChildAdapterPosition(item) == position
        }
    }

    private fun accessibilityAction(actionId: Int) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription() = "perform accessibility action $actionId"
        override fun perform(uiController: UiController, view: View) {
            ViewCompat.performAccessibilityAction(view, actionId, null)
            uiController.loopMainThreadUntilIdle()
        }
    }
}
