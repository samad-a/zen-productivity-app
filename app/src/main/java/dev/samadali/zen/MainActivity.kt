package dev.samadali.zen

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }
}
