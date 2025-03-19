package dev.samadali.zen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigation.setItemActiveIndicatorEnabled(false)

        bottomNavigation.selectedItemId = R.id.pomodoro

        if (savedInstanceState == null) {
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

            supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit()

            true
        }
    }
}