package dev.samadali.zen.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import dev.samadali.zen.ZenApp
import dev.samadali.zen.pomodoro.PomodoroTimer
import org.hamcrest.Matchers.endsWith

/** Shared setup for the Espresso screen tests. */
object UiTestSupport {
    val context: Context get() = ApplicationProvider.getApplicationContext()

    init {
        // Every Espresso action also runs the Accessibility Test Framework checks
        // (touch target size, labels, contrast) on the whole screen.
        AccessibilityChecks.enable()
            .setRunChecksFromRootView(true)
            // The platform CalendarView's month grid has no label. It is replaced by
            // the heatmap calendar, after which this exception should be removed.
            .setSuppressingResultMatcher(
                matchesViews(withClassName(endsWith("SimpleMonthView")))
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
            PomodoroTimer.init(context)
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
