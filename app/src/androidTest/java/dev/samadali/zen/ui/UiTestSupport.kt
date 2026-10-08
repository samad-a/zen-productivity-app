package dev.samadali.zen.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheckNames
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import dev.samadali.zen.R
import dev.samadali.zen.ZenApp
import dev.samadali.zen.data.FocusSession
import dev.samadali.zen.pomodoro.PomodoroTimer
import dev.samadali.zen.settings.AppSettings
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import java.time.LocalDate
import java.time.ZoneId

/** Shared setup for the Espresso screen tests. */
object UiTestSupport {
    val context: Context get() = ApplicationProvider.getApplicationContext()

    init {
        // Every Espresso action also runs the Accessibility Test Framework checks
        // (touch target size, labels, contrast) on the whole screen.
        AccessibilityChecks.enable()
            .setRunChecksFromRootView(true)
            // Calendar days are 48dp tall but share the width between seven columns, so on
            // narrow phones (CI's emulator is 320dp wide) they're about 35dp wide. That still
            // passes WCAG 2.2's 24dp minimum target size; nothing else is exempt.
            .setSuppressingResultMatcher(
                allOf(
                    matchesCheckNames(`is`("TouchTargetSizeCheck")),
                    matchesViews(withId(R.id.dayText))
                )
            )
    }

    /** Lets the timer start its foreground service without a permission dialog. */
    fun notificationPermission(): GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    /** Empties the database and puts the timer back to its defaults. */
    fun resetAppState() {
        (context as ZenApp).database.clearAllTables()
        onMain {
            PomodoroTimer.pause()
            PomodoroTimer.clock = SystemClock::elapsedRealtime
            context.getSharedPreferences("pomodoro", Context.MODE_PRIVATE).edit().clear().commit()
            AppSettings(context).theme = AppSettings.Theme.SYSTEM
            PomodoroTimer.init(context)
        }
    }

    /** Adds a completed 25 minute study session at noon, [daysAgo] days before today. */
    fun addSession(daysAgo: Long) {
        val completedAt = LocalDate.now().minusDays(daysAgo).atTime(12, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runBlocking {
            (context as ZenApp).database.focusSessionDao()
                .insert(FocusSession(startedAt = completedAt - 25 * 60_000, completedAt = completedAt, durationMillis = 25 * 60_000))
        }
    }

    fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    /**
     * Retries [assertion] until it passes or [timeoutMs] runs out. Room delivers LiveData
     * updates from a background thread, which Espresso doesn't wait for.
     */
    fun eventually(timeoutMs: Long = 5_000, assertion: () -> Unit) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            try {
                assertion()
                return
            } catch (e: Throwable) {
                if (SystemClock.uptimeMillis() > deadline) throw e
                Thread.sleep(50)
            }
        }
    }
}
