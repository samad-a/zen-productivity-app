package dev.samadali.zen

import android.app.Application

class ZenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PomodoroTimer.init(this)
    }
}
