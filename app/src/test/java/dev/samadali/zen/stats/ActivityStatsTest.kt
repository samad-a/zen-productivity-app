package dev.samadali.zen.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class ActivityStatsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 8) // a Thursday

    private fun at(date: LocalDate, hour: Int = 12): Long =
        LocalDateTime.of(date, LocalTime.of(hour, 0)).toInstant(zone).toEpochMilli()

    private fun session(date: LocalDate, minutes: Long = 25) = SessionRecord(at(date), minutes * 60_000)

    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun heatLevel_matchesTheLegend() {
        assertEquals(0, ActivityStats.heatLevel(0))
        assertEquals(1, ActivityStats.heatLevel(1))
        assertEquals(2, ActivityStats.heatLevel(2))
        assertEquals(2, ActivityStats.heatLevel(3))
        assertEquals(3, ActivityStats.heatLevel(4))
        assertEquals(3, ActivityStats.heatLevel(12))
    }

    @Test
    fun currentStreak_countsBackFromToday() {
        assertEquals(3, ActivityStats.currentStreak(days(0, 1, 2, 4), today))
    }

    @Test
    fun currentStreak_isKeptUntilMidnightIfYesterdayCounted() {
        assertEquals(2, ActivityStats.currentStreak(days(1, 2), today))
    }

    @Test
    fun currentStreak_isZeroAfterAMissedDay() {
        assertEquals(0, ActivityStats.currentStreak(days(2, 3), today))
        assertEquals(0, ActivityStats.currentStreak(emptySet(), today))
    }

    @Test
    fun longestStreak_findsTheLongestRun() {
        assertEquals(4, ActivityStats.longestStreak(days(0, 1, 5, 6, 7, 8, 10)))
        assertEquals(0, ActivityStats.longestStreak(emptySet()))
    }

    @Test
    fun byDay_groupsSessionsAndTasksByLocalDate() {
        val yesterday = today.minusDays(1)
        val result = ActivityStats.byDay(
            sessions = listOf(session(today), session(today, 50), session(yesterday)),
            tasks = listOf(TaskRecord("Gym", at(today)), TaskRecord("Read", null)),
            zone = zone
        )

        assertEquals(DayActivity(sessions = 2, focusMillis = 75 * 60_000, tasksCompleted = 1), result[today])
        assertEquals(1, result.getValue(yesterday).sessions)
        assertEquals(2, result.size)
    }

    @Test
    fun byDay_usesTheGivenTimeZone() {
        // 23:30 UTC on the 7th is already the 8th in UTC+2
        val lateEvening = LocalDateTime.of(2026, 10, 7, 23, 30).toInstant(zone).toEpochMilli()
        val result = ActivityStats.byDay(listOf(SessionRecord(lateEvening, 1)), emptyList(), ZoneOffset.ofHours(2))

        assertTrue(result.containsKey(today))
    }

    @Test
    fun summary_combinesEverything() {
        val monday = LocalDate.of(2026, 10, 5)
        val summary = ActivityStats.summary(
            sessions = listOf(session(monday), session(monday), session(today.minusDays(1)), session(today)),
            tasks = listOf(
                TaskRecord("Gym", at(today)),
                TaskRecord("gym ", at(monday)),
                TaskRecord("Read", null)
            ),
            joinedAt = at(LocalDate.of(2026, 9, 28)),
            today = today,
            zone = zone
        )

        assertEquals(2, summary.tasksCompleted)
        assertEquals(100 * 60_000L, summary.focusMillis)
        assertEquals(2, summary.currentStreak)
        assertEquals(2, summary.longestStreak)
        assertTrue(summary.studiedToday)
        assertEquals(10, summary.daysSinceJoining)
        assertEquals(DayOfWeek.MONDAY, summary.mostProductiveDay)
        assertEquals(2, summary.uniqueTasks)
    }

    @Test
    fun summary_withNoActivity() {
        val summary = ActivityStats.summary(emptyList(), emptyList(), at(today), today, zone)

        assertEquals(0, summary.currentStreak)
        assertFalse(summary.studiedToday)
        assertNull(summary.mostProductiveDay)
        assertEquals(0, summary.daysSinceJoining)
    }
}
