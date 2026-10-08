package dev.samadali.zen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.samadali.zen.calendar.CalendarFragment
import dev.samadali.zen.databinding.ActivityMainBinding
import dev.samadali.zen.pomodoro.PomodoroFragment
import dev.samadali.zen.settings.SettingsFragment
import dev.samadali.zen.stats.StatsFragment
import dev.samadali.zen.tasks.TasksFragment

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null && animationScale() > 0f) playIntro(splashScreen, binding)

        val bottomNavigation = binding.bottomNavigationView
        // The navigation bar pads itself for the gesture/nav bar; the screens above it
        // need room for the status bar, camera cutout and, while typing, the keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(binding.flFragment) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = (ime.bottom - bottomNavigation.height).coerceAtLeast(0)
            )
            insets
        }
        bottomNavigation.setItemActiveIndicatorEnabled(false)

        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.tasks -> TasksFragment()
                R.id.calendar -> CalendarFragment()
                R.id.stats -> StatsFragment()
                R.id.settings -> SettingsFragment()
                else -> PomodoroFragment()
            }
            showTab(fragment)
            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = intent.getIntExtra(EXTRA_TAB, R.id.pomodoro)
        }
        this.bottomNavigation = bottomNavigation
    }

    /**
     * The launch animation: the system splash screen opens the logo's petals (Android 12+),
     * then the overlay, showing the same logo in the same place, fades in the title and
     * fades away to reveal the app.
     */
    private fun playIntro(splashScreen: SplashScreen, binding: ActivityMainBinding) {
        val overlay = binding.introOverlay
        overlay.isVisible = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Keep the splash screen up until the petals have finished opening
            var opening = true
            splashScreen.setKeepOnScreenCondition { opening }
            // Animations run at the system speed setting, so scale the wait to match
            overlay.postDelayed({ opening = false }, (PETALS_OPEN_MS * animationScale()).toLong())
        }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
            binding.introTitle.animate()
                .alpha(1f)
                .setDuration(TITLE_FADE_MS)
                .withEndAction {
                    overlay.animate()
                        .alpha(0f)
                        .setStartDelay(TITLE_HOLD_MS)
                        .setDuration(OVERLAY_FADE_MS)
                        .withEndAction { overlay.isVisible = false }
                }
        }
    }

    /** The system animation speed; 0 when the user has turned animations off (and in UI tests). */
    private fun animationScale(): Float =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

    // Opened from a notification while already running, e.g. a task reminder
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tab = intent.getIntExtra(EXTRA_TAB, 0)
        if (tab != 0) bottomNavigation.selectedItemId = tab
    }

    private fun showTab(fragment: Fragment) {
        // Drop sub-screens such as TaskEditorFragment so back doesn't return to a different tab
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragment, fragment)
            .commit()
    }

    companion object {
        /** Menu id of the tab to open, e.g. R.id.tasks. */
        const val EXTRA_TAB = "dev.samadali.zen.extra.TAB"

        // Matches the petal animation in drawable-v31/avd_splash_logo.xml
        private const val PETALS_OPEN_MS = 750L
        private const val TITLE_FADE_MS = 350L
        private const val TITLE_HOLD_MS = 300L
        private const val OVERLAY_FADE_MS = 350L
    }
}
