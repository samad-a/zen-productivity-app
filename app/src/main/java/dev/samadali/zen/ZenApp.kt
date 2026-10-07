package dev.samadali.zen

import android.app.Application

class ZenApp : Application() {
    val database: ZenDatabase by lazy { ZenDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        PomodoroTimer.init(this)
    }
}
