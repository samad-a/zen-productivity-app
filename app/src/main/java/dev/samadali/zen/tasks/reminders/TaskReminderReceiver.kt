package dev.samadali.zen.tasks.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import dev.samadali.zen.ZenApp
import dev.samadali.zen.settings.AppSettings
import kotlinx.coroutines.launch

/** Posts a task's reminder when its alarm fires, unless the task was finished meanwhile. */
class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TaskReminders.EXTRA_TASK_ID, -1)
        if (taskId < 0 || !AppSettings(context).taskReminders) return
        val app = context.applicationContext as ZenApp
        val pending = goAsync()
        app.applicationScope.launch {
            try {
                val task = app.database.taskDao().getById(taskId)
                if (task != null && !task.isCompleted) notify(context, task.id, task.name)
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, taskId: Long, name: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        TaskReminders.createChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, R.id.tasks)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, TaskReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.checklist)
            .setContentTitle(name)
            .setContentText(context.getString(R.string.task_reminder_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        // Offset so reminders don't replace the timer's notifications (ids 1 and 2)
        manager.notify(NOTIFICATION_ID_BASE + taskId.toInt(), notification)
    }

    private companion object {
        const val NOTIFICATION_ID_BASE = 1000
    }
}
