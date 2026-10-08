package dev.samadali.zen.tasks.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import dev.samadali.zen.R
import dev.samadali.zen.data.Task

/**
 * Schedules a notification at a task's reminder time. Uses an inexact alarm that still fires
 * in Doze, so no exact-alarm permission is needed; it may arrive a few minutes late.
 */
object TaskReminders {
    const val CHANNEL_ID = "task_reminders"
    const val EXTRA_TASK_ID = "task_id"

    /** Schedules [task]'s reminder, or cancels it if the task no longer needs one. */
    fun sync(context: Context, task: Task, now: Long = System.currentTimeMillis()) {
        val remindAt = task.remindAt
        if (task.isCompleted || remindAt == null || remindAt <= now) {
            cancel(context, task.id)
            return
        }
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, remindAt, intent(context, task.id))
    }

    fun cancel(context: Context, taskId: Long) {
        ContextCompat.getSystemService(context, AlarmManager::class.java)?.cancel(intent(context, taskId))
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_task_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun intent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).putExtra(EXTRA_TASK_ID, taskId)
        // One alarm per task: the request code keeps them apart
        return PendingIntent.getBroadcast(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
