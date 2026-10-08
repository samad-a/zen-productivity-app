package dev.samadali.zen.pomodoro

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Observer
import dev.samadali.zen.MainActivity
import dev.samadali.zen.R

/**
 * Foreground service that keeps the pomodoro timer alive while the app is in the
 * background, shows the ongoing countdown with pause and skip actions, and alerts the
 * user when a phase ends.
 */
class PomodoroService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private var observing = false

    private val handler = Handler(Looper.getMainLooper())

    // Checked after a post, so skipping (a pause immediately followed by a start)
    // doesn't stop the service in between.
    private val runningObserver = Observer<Boolean> { running ->
        if (!running) handler.post { if (PomodoroTimer.isRunning.value != true) stopSelf() }
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
        PomodoroTimer.onPhaseFinished = ::onPhaseFinished
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Required promptly after startForegroundService, even if we stop straight away
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, ONGOING_ID, buildOngoingNotification(), type)

        when (intent?.action) {
            ACTION_PAUSE -> PomodoroTimer.pause()
            ACTION_SKIP -> PomodoroTimer.skip(this)
        }
        if (PomodoroTimer.isRunning.value != true) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!observing) {
            observing = true
            PomodoroTimer.isRunning.observeForever(runningObserver)
            PomodoroTimer.phase.observeForever(phaseObserver)
        }
        refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        PomodoroTimer.onPhaseFinished = null
        PomodoroTimer.isRunning.removeObserver(runningObserver)
        PomodoroTimer.phase.removeObserver(phaseObserver)
        if (wakeLock.isHeld) wakeLock.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Updates the countdown notification and keeps the CPU awake until the phase ends. */
    @SuppressLint("MissingPermission")
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
            .setContentTitle(getString(phaseInProgressText(PomodoroTimer.phase.value ?: Phase.STUDY)))
            .setWhen(PomodoroTimer.phaseEndsAtWallClock())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(R.drawable.ic_pause, getString(R.string.pause), serviceIntent(ACTION_PAUSE))
            .addAction(R.drawable.ic_skip_next, getString(R.string.skip), serviceIntent(ACTION_SKIP))
            .build()

    private fun onPhaseFinished(finished: Phase) {
        PhaseAlert.play(this, PomodoroTimer.settings)
        postPhaseFinishedNotification(finished)
    }

    @SuppressLint("MissingPermission")
    private fun postPhaseFinishedNotification(finished: Phase) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val text = if (finished == Phase.STUDY) R.string.notification_study_finished
        else R.string.notification_break_finished
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.clock)
            .setContentTitle(getString(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // Sound and vibration are played by PhaseAlert, following the user's settings
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        manager.notify(ALERT_ID, notification)
    }

    private fun phaseInProgressText(phase: Phase) = when (phase) {
        Phase.STUDY -> R.string.notification_study_in_progress
        Phase.BREAK -> R.string.notification_break_in_progress
        Phase.LONG_BREAK -> R.string.notification_long_break_in_progress
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun serviceIntent(action: String): PendingIntent {
        val intent = Intent(this, PomodoroService::class.java).setAction(action)
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
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
        // The first alerts channel made its own sound; this one is silent so the
        // sound and vibration settings can apply.
        manager.deleteNotificationChannel(OLD_CHANNEL_ALERTS)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.channel_phase_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    companion object {
        private const val CHANNEL_ONGOING = "pomodoro_timer"
        private const val OLD_CHANNEL_ALERTS = "pomodoro_alerts"
        private const val CHANNEL_ALERTS = "pomodoro_phase_alerts"
        private const val ONGOING_ID = 1
        private const val ALERT_ID = 2
        private const val WAKE_LOCK_MARGIN_MS = 30_000L
        private const val ACTION_PAUSE = "dev.samadali.zen.action.PAUSE"
        private const val ACTION_SKIP = "dev.samadali.zen.action.SKIP"
    }
}
