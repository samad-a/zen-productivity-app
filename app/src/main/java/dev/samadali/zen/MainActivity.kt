package dev.samadali.zen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.samadali.zen.calendar.CalendarFragment
import dev.samadali.zen.pomodoro.PomodoroFragment
import dev.samadali.zen.settings.SettingsFragment
import dev.samadali.zen.stats.StatsFragment
import dev.samadali.zen.tasks.TasksFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigation.setItemActiveIndicatorEnabled(false)

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.pomodoro
            supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, PomodoroFragment())
                .commit()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.pomodoro -> PomodoroFragment()
                R.id.tasks -> TasksFragment()
                R.id.calendar -> CalendarFragment()
                R.id.stats -> StatsFragment()
                R.id.settings -> SettingsFragment()
                else -> PomodoroFragment()
            }

            // Drop sub-screens such as AddTaskFragment so back doesn't return to a different tab
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit()

            true
        }
    }
}
