package dev.samadali.zen.tasks.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.samadali.zen.ZenApp
import kotlinx.coroutines.launch

/** Alarms are cleared on reboot and app updates, so schedule the upcoming reminders again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as ZenApp
        val pending = goAsync()
        app.applicationScope.launch {
            try {
                app.database.taskDao().getUpcomingReminders(System.currentTimeMillis())
                    .forEach { TaskReminders.sync(context, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
