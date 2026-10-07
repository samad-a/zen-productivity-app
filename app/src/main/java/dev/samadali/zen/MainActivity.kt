package dev.samadali.zen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.samadali.zen.calendar.CalendarFragment
import dev.samadali.zen.databinding.ActivityMainBinding
import dev.samadali.zen.pomodoro.PomodoroFragment
import dev.samadali.zen.settings.SettingsFragment
import dev.samadali.zen.stats.StatsFragment
import dev.samadali.zen.tasks.TasksFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNavigation = binding.bottomNavigationView
        bottomNavigation.setItemActiveIndicatorEnabled(false)

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.pomodoro
            showTab(PomodoroFragment())
        }

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
    }

    private fun showTab(fragment: Fragment) {
        // Drop sub-screens such as AddTaskFragment so back doesn't return to a different tab
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragment, fragment)
            .commit()
    }
}
