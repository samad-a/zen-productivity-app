package dev.samadali.zen.settings

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R
import dev.samadali.zen.ZenApp
import dev.samadali.zen.stats.ActivityStats
import dev.samadali.zen.stats.SessionRecord
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The optional daily notifications: a streak reminder in the evening and a study tip in the
 * morning. Each alarm reschedules itself for the next day when it fires.
 */
object DailyNotifications {
    enum class Kind(val time: LocalTime, val requestCode: Int) {
        STREAK(LocalTime.of(20, 0), 1),
        MOTIVATION(LocalTime.of(8, 0), 2)
    }

    const val CHANNEL_ID = "daily_nudges"
    private const val EXTRA_KIND = "kind"
    private const val NOTIFICATION_ID_BASE = 900

    /** Schedules or cancels both notifications to match the settings. */
    fun sync(context: Context) {
        val settings = AppSettings(context)
        sync(context, Kind.STREAK, settings.streakReminders)
        sync(context, Kind.MOTIVATION, settings.studyMotivation)
    }

    private fun sync(context: Context, kind: Kind, enabled: Boolean) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        val intent = pendingIntent(context, kind)
        if (!enabled) {
            alarmManager.cancel(intent)
            return
        }
        AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, nextTime(kind.time), intent)
    }

    /** The next time today or tomorrow at [time], in the device's time zone. */
    fun nextTime(time: LocalTime, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var next = now.with(time).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_daily_nudges), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun pendingIntent(context: Context, kind: Kind): PendingIntent =
        PendingIntent.getBroadcast(
            context, kind.requestCode,
            Intent(context, Receiver::class.java).putExtra(EXTRA_KIND, kind.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val kind = Kind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_KIND) } ?: return
            val app = context.applicationContext as ZenApp
            val pending = goAsync()
            app.applicationScope.launch {
                try {
                    val text = when (kind) {
                        Kind.STREAK -> streakText(app)
                        Kind.MOTIVATION -> context.resources.getStringArray(R.array.study_tips).random()
                    }
                    if (text != null) post(context, kind, text)
                } finally {
                    sync(context)
                    pending.finish()
                }
            }
        }

        /** Only nudges when there's a streak to keep and today has no session yet. */
        private suspend fun streakText(app: ZenApp): String? {
            val sessions = app.database.focusSessionDao().getAllOnce().map { SessionRecord(it.completedAt, it.durationMillis) }
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val studyDays = ActivityStats.byDay(sessions, emptyList(), zone).keys
            if (today in studyDays) return null
            val streak = ActivityStats.currentStreak(studyDays, today)
            if (streak == 0) return null
            return app.resources.getQuantityString(R.plurals.streak_reminder_text, streak, streak)
        }

        @SuppressLint("MissingPermission")
        private fun post(context: Context, kind: Kind, text: String) {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return
            createChannel(context)
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.clock)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            manager.notify(NOTIFICATION_ID_BASE + kind.requestCode, notification)
        }
    }
}
