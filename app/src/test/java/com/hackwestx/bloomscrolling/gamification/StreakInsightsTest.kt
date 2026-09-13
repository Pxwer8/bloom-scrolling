package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// Same fixed-date approach as StreakCalculatorTest, for the same reason:
// LocalDate.now() would make these tests flaky around month/year rollovers.
private val TODAY = LocalDate.of(2026, 9, 12)
private const val INSTAGRAM = "com.instagram.android"
private const val LIMIT_MINUTES = 30

private fun day(daysAgo: Int): String = TODAY.minusDays(daysAgo.toLong()).toString()

private fun usage(daysAgo: Int, minutes: Int, packageName: String = INSTAGRAM) =
    UsageLog(date = day(daysAgo), packageName = packageName, totalMinutes = minutes, pickupCount = 1)

private fun monitored(packageName: String = INSTAGRAM, limit: Int = LIMIT_MINUTES) =
    UserSettings(packageName = packageName, dailyLimitMinutes = limit, isBlocked = true)

class StreakInsightsTest {

    @Test
    fun `longest streak finds a past run even after the current streak broke`() {
        val logs = listOf(
            // A 4-day streak, 3-6 days ago.
            usage(daysAgo = 6, minutes = 10),
            usage(daysAgo = 5, minutes = 10),
            usage(daysAgo = 4, minutes = 10),
            usage(daysAgo = 3, minutes = 10),
            // Explicitly broken on days 1-2 (not just "no data" — a missing
            // day would count as 0 minutes used, i.e. within limit, which
            // would extend the streak instead of breaking it).
            usage(daysAgo = 2, minutes = 99),
            usage(daysAgo = 1, minutes = 99),
            // Today is under limit again (current streak of 1).
            usage(daysAgo = 0, minutes = 5)
        )
        val settings = listOf(monitored())

        assertEquals(1, calculateStreak(logs, settings, TODAY))
        assertEquals(4, calculateLongestStreak(logs, settings, TODAY))
    }

    @Test
    fun `longest streak is zero with no logs`() {
        assertEquals(0, calculateLongestStreak(emptyList(), listOf(monitored()), TODAY))
    }

    @Test
    fun `week strip marks each day independently, not stopping at the first break`() {
        val logs = listOf(
            usage(daysAgo = 6, minutes = 10), // held
            usage(daysAgo = 5, minutes = 99), // broken
            usage(daysAgo = 4, minutes = 10)  // held
            // days 3..0: no log rows = zero minutes = held
        )
        val settings = listOf(monitored())

        val statuses = lastWeekDayStatuses(logs, settings, TODAY)

        assertEquals(7, statuses.size)
        assertEquals(day(6), statuses[0].date.toString())
        assertEquals(true, statuses[0].heldLimit)
        assertEquals(false, statuses[1].heldLimit)
        assertEquals(true, statuses[2].heldLimit)
        assertEquals(true, statuses.last().heldLimit) // today, no rows logged
    }

    @Test
    fun `week strip shows nothing held when no apps are monitored`() {
        val statuses = lastWeekDayStatuses(logs = emptyList(), settings = emptyList(), today = TODAY)
        assertEquals(7, statuses.size)
        assertEquals(true, statuses.none { it.heldLimit })
    }
}
