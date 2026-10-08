package dev.samadali.zen.stats

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the user did on one day. A day with at least one session counts towards the streak. */
data class DayActivity(
    val sessions: Int = 0,
    val focusMillis: Long = 0,
    val tasksCompleted: Int = 0
) {
    val isStudyDay: Boolean get() = sessions > 0
}

/** A completed study session, reduced to what the stats need. */
data class SessionRecord(val completedAt: Long, val durationMillis: Long)

/** A task, reduced to what the stats need. */
data class TaskRecord(val name: String, val completedAt: Long?)

data class StatsSummary(
    val tasksCompleted: Int,
    val focusMillis: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    /** True once today has a session, so the streak is safe for today. */
    val studiedToday: Boolean,
    val daysSinceJoining: Long,
    /** Weekday with the most sessions, or null before the first session. */
    val mostProductiveDay: DayOfWeek?,
    val uniqueTasks: Int
)

object ActivityStats {

    /** Heatmap shade for a day: 0 none, 1 for 1 session, 2 for 2-3, 3 for 4 or more. */
    fun heatLevel(sessions: Int): Int = when {
        sessions <= 0 -> 0
        sessions == 1 -> 1
        sessions <= 3 -> 2
        else -> 3
    }

    fun byDay(sessions: List<SessionRecord>, tasks: List<TaskRecord>, zone: ZoneId): Map<LocalDate, DayActivity> {
        val days = mutableMapOf<LocalDate, DayActivity>()
        for (session in sessions) {
            val day = session.completedAt.toLocalDate(zone)
            val current = days[day] ?: DayActivity()
            days[day] = current.copy(
                sessions = current.sessions + 1,
                focusMillis = current.focusMillis + session.durationMillis
            )
        }
        for (task in tasks) {
            val day = task.completedAt?.toLocalDate(zone) ?: continue
            val current = days[day] ?: DayActivity()
            days[day] = current.copy(tasksCompleted = current.tasksCompleted + 1)
        }
        return days
    }

    /**
     * Consecutive study days up to today. A streak that reached yesterday still counts
     * today, since the user has until midnight to keep it going.
     */
    fun currentStreak(studyDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in studyDays) today else today.minusDays(1)
        var streak = 0
        while (day in studyDays) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun longestStreak(studyDays: Set<LocalDate>): Int {
        var longest = 0
        for (day in studyDays) {
            // Only count forwards from the first day of each run
            if (day.minusDays(1) in studyDays) continue
            var length = 0
            var next = day
            while (next in studyDays) {
                length++
                next = next.plusDays(1)
            }
            longest = maxOf(longest, length)
        }
        return longest
    }

    fun summary(
        sessions: List<SessionRecord>,
        tasks: List<TaskRecord>,
        joinedAt: Long,
        today: LocalDate,
        zone: ZoneId
    ): StatsSummary {
        val days = byDay(sessions, tasks, zone)
        val studyDays = days.filterValues { it.isStudyDay }.keys
        val mostProductiveDay = sessions
            .groupingBy { it.completedAt.toLocalDate(zone).dayOfWeek }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<DayOfWeek, Int>> { it.value }.thenByDescending { it.key })
            ?.key
        return StatsSummary(
            tasksCompleted = tasks.count { it.completedAt != null },
            focusMillis = sessions.sumOf { it.durationMillis },
            currentStreak = currentStreak(studyDays, today),
            longestStreak = longestStreak(studyDays),
            studiedToday = today in studyDays,
            daysSinceJoining = (today.toEpochDay() - joinedAt.toLocalDate(zone).toEpochDay()).coerceAtLeast(0),
            mostProductiveDay = mostProductiveDay,
            uniqueTasks = tasks.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.toSet().size
        )
    }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}
