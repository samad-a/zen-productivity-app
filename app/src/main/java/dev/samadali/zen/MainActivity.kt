package dev.samadali.zen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigation.setItemActiveIndicatorEnabled(false)

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.pomodoro
            supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, Pomodoro())
                .commit()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.pomodoro -> Pomodoro()
                R.id.tasks -> Tasks()
                R.id.calendar -> Calendar()
                R.id.stats -> Stats()
                R.id.settings -> Settings()
                else -> Pomodoro()
            }

            // Drop sub-screens such as AddTask so back doesn't return to a different tab
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit()

            true
        }
    }
}