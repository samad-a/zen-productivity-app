package dev.samadali.zen.tasks

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dev.samadali.zen.ZenApp
import dev.samadali.zen.data.Task
import dev.samadali.zen.tasks.reminders.TaskReminderReceiver
import dev.samadali.zen.tasks.reminders.TaskReminders
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fires the reminder receiver directly, as the alarm would. */
@RunWith(AndroidJUnit4::class)
class TaskReminderTest {
    @get:Rule
    val permission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    private val context: ZenApp = ApplicationProvider.getApplicationContext()
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        context.database.clearAllTables()
        notifications.cancelAll()
    }

    @After
    fun tearDown() {
        notifications.cancelAll()
        context.database.clearAllTables()
    }

    @Test
    fun reminderShowsANotificationWithTheTaskName() {
        val id = insert(Task(name = "Revise maths", description = "", remindAt = System.currentTimeMillis()))

        fireReminder(id)

        assertTrue(waitFor { notificationTitles().contains("Revise maths") })
    }

    @Test
    fun reminderIsSkippedIfTheTaskIsAlreadyDone() {
        val id = insert(Task(name = "Gym", description = "", isCompleted = true, remindAt = System.currentTimeMillis()))

        fireReminder(id)

        assertFalse(waitFor(timeoutMs = 1_500) { notificationTitles().contains("Gym") })
    }

    private fun insert(task: Task): Long = runBlocking { context.database.taskDao().insert(task) }

    private fun fireReminder(taskId: Long) {
        context.sendBroadcast(
            Intent(context, TaskReminderReceiver::class.java).putExtra(TaskReminders.EXTRA_TASK_ID, taskId)
        )
    }

    private fun notificationTitles(): List<String> = notifications.activeNotifications
        .mapNotNull { it.notification.extras.getCharSequence("android.title")?.toString() }

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }
}
