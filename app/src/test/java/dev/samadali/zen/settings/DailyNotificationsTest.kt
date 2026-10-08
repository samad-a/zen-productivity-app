package dev.samadali.zen.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DailyNotificationsTest {
    private fun at(hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, 8, hour, minute, 0, 0, ZoneOffset.UTC)

    @Test
    fun nextTime_isLaterTodayIfStillAhead() {
        assertEquals(at(20).toInstant().toEpochMilli(), DailyNotifications.nextTime(LocalTime.of(20, 0), at(9)))
    }

    @Test
    fun nextTime_isTomorrowOnceTodaysTimeHasPassed() {
        assertEquals(
            at(8).plusDays(1).toInstant().toEpochMilli(),
            DailyNotifications.nextTime(LocalTime.of(8, 0), at(8))
        )
        assertEquals(
            at(8).plusDays(1).toInstant().toEpochMilli(),
            DailyNotifications.nextTime(LocalTime.of(8, 0), at(21, 30))
        )
    }
}
