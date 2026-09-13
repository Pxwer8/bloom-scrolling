package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 9, 12) // 2026-09-12
private const val INSTAGRAM = "com.instagram.android"

private fun monitored(packageName: String = INSTAGRAM, limit: Int) =
    UserSettings(packageName = packageName, dailyLimitMinutes = limit, isBlocked = true)

class TimeSavedTest {

    @Test
    fun `no monitored apps means nothing to save`() {
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-12", INSTAGRAM, totalMinutes = 5, pickupCount = 1)),
            settings = emptyList(),
            today = TODAY
        )
        assertEquals(0, summary.percentSavedThisMonth)
        assertEquals(0.0, summary.weeksSaved, 0.0)
        assertEquals(0, summary.projectedHoursPerYear)
    }

    @Test
    fun `going over the limit saves nothing, never negative`() {
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-12", INSTAGRAM, totalMinutes = 90, pickupCount = 1)),
            settings = listOf(monitored(limit = 30)),
            today = TODAY
        )
        assertEquals(0, summary.percentSavedThisMonth)
        assertEquals(1, summary.daysActive)
    }

    @Test
    fun `percent saved this month is the ratio of saved minutes to the monthly limit budget`() {
        // Limit 30, used 10 -> saved 20 out of a 30-minute budget that day = ~67%.
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-12", INSTAGRAM, totalMinutes = 10, pickupCount = 1)),
            settings = listOf(monitored(limit = 30)),
            today = TODAY
        )
        assertEquals(67, summary.percentSavedThisMonth)
    }

    @Test
    fun `days from a different month do not count toward this month's percentage`() {
        val summary = calculateTimeSaved(
            logs = listOf(
                UsageLog("2026-08-15", INSTAGRAM, totalMinutes = 0, pickupCount = 0), // last month, full save
                UsageLog("2026-09-12", INSTAGRAM, totalMinutes = 30, pickupCount = 1) // this month, zero saved
            ),
            settings = listOf(monitored(limit = 30)),
            today = TODAY
        )
        // Only the September row should count toward this month's percentage.
        assertEquals(0, summary.percentSavedThisMonth)
        // But both days still count toward all-time stats.
        assertEquals(2, summary.daysActive)
    }

    @Test
    fun `weeks saved converts all-time saved minutes into 7-day-week units`() {
        // 7 days, each saving 1440 minutes (a full day) = one full week saved.
        val logs = (0..6).map { UsageLog(TODAY.minusDays(it.toLong()).toString(), INSTAGRAM, totalMinutes = 0, pickupCount = 0) }
        val summary = calculateTimeSaved(logs = logs, settings = listOf(monitored(limit = 1440)), today = TODAY)
        assertEquals(1.0, summary.weeksSaved, 0.001)
    }
}
