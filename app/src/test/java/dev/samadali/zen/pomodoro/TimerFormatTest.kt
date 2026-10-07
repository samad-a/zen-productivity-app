package dev.samadali.zen.pomodoro

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerFormatTest {

    @Test
    fun formatRemaining_formatsMinutesAndSeconds() {
        assertEquals("25:00", formatRemaining(25 * 60 * 1000L))
        assertEquals("04:32", formatRemaining((4 * 60 + 32) * 1000L))
        assertEquals("00:00", formatRemaining(0L))
    }

    @Test
    fun formatRemaining_roundsPartialSecondsDown() {
        assertEquals("00:59", formatRemaining(59_999L))
    }

    @Test
    fun formatRemaining_supportsDurationsOverAnHour() {
        assertEquals("90:00", formatRemaining(90 * 60 * 1000L))
    }

    @Test
    fun formatRemaining_clampsNegativeToZero() {
        assertEquals("00:00", formatRemaining(-500L))
    }

    @Test
    fun progressPercent_tracksElapsedFraction() {
        assertEquals(0, progressPercent(remainingMillis = 1000, totalMillis = 1000))
        assertEquals(50, progressPercent(remainingMillis = 500, totalMillis = 1000))
        assertEquals(100, progressPercent(remainingMillis = 0, totalMillis = 1000))
    }

    @Test
    fun progressPercent_handlesZeroTotal() {
        assertEquals(0, progressPercent(remainingMillis = 0, totalMillis = 0))
    }

    @Test
    fun progressPercent_clampsOutOfRangeValues() {
        assertEquals(0, progressPercent(remainingMillis = 2000, totalMillis = 1000))
        assertEquals(100, progressPercent(remainingMillis = -10, totalMillis = 1000))
    }
}
