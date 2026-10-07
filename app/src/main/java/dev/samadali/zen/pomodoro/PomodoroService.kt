package dev.samadali.zen.pomodoro

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Observer
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R

/**
 * Foreground service that keeps the pomodoro timer alive while the app is in the
 * background, shows the ongoing countdown and alerts the user when a phase ends.
 */
class PomodoroService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private var observing = false

    private val runningObserver = Observer<Boolean> { running ->
        if (!running) stopSelf()
    }

    private val phaseObserver = Observer<Phase> {
        refresh()
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zen:pomodoro")
            .apply { setReferenceCounted(false) }
        PomodoroTimer.onPhaseFinished = ::notifyPhaseFinished
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, ONGOING_ID, buildOngoingNotification(), type)

        if (!observing) {
            observing = true
            PomodoroTimer.isRunning.observeForever(runningObserver)
            PomodoroTimer.phase.observeForever(phaseObserver)
        }
        refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        PomodoroTimer.onPhaseFinished = null
        PomodoroTimer.isRunning.removeObserver(runningObserver)
        PomodoroTimer.phase.removeObserver(phaseObserver)
        if (wakeLock.isHeld) wakeLock.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Updates the countdown notification and keeps the CPU awake until the phase ends. */
    @SuppressLint("MissingPermission", "WakelockTimeout")
    private fun refresh() {
        if (PomodoroTimer.isRunning.value != true) return
        wakeLock.acquire(PomodoroTimer.remainingNow() + WAKE_LOCK_MARGIN_MS)
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(ONGOING_ID, buildOngoingNotification())
        }
    }

    private fun buildOngoingNotification() =
        NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.clock)
            .setContentTitle(
                if (PomodoroTimer.phase.value == Phase.BREAK) getString(R.string.notification_break_in_progress)
                else getString(R.string.notification_study_in_progress)
            )
            .setWhen(PomodoroTimer.phaseEndsAtWallClock())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .build()

    @SuppressLint("MissingPermission")
    private fun notifyPhaseFinished(finished: Phase) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val text = if (finished == Phase.STUDY) R.string.notification_study_finished
        else R.string.notification_break_finished
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.clock)
            .setContentTitle(getString(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        manager.notify(ALERT_ID, notification)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                getString(R.string.channel_timer),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.channel_phase_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { enableVibration(true) }
        )
    }

    companion object {
        private const val CHANNEL_ONGOING = "pomodoro_timer"
        private const val CHANNEL_ALERTS = "pomodoro_alerts"
        private const val ONGOING_ID = 1
        private const val ALERT_ID = 2
        private const val WAKE_LOCK_MARGIN_MS = 30_000L
    }
}
