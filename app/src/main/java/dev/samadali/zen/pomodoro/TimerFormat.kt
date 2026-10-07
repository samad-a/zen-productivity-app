package dev.samadali.zen.pomodoro

import java.util.Locale

/** Formats a remaining duration as mm:ss. */
fun formatRemaining(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1000
    return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/** How far through a phase we are, from 0 to 100. */
fun progressPercent(remainingMillis: Long, totalMillis: Long): Int {
    if (totalMillis <= 0) return 0
    val elapsed = (totalMillis - remainingMillis).coerceIn(0L, totalMillis)
    return (elapsed * 100 / totalMillis).toInt()
}
