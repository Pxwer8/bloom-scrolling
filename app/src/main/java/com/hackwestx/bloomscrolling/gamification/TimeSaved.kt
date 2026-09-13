package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import java.time.LocalDate
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// PLACEHOLDER FORMULA — nothing in the schema or the team's plan defines what
// "time saved" means numerically. This is one reasonable interpretation,
// not a product decision:
//   - per (app, day): minutes saved = max(0, dailyLimitMinutes - actual
//     minutes that day). Going over the limit saves nothing (never negative).
//   - "% saved this month" = month's total saved minutes / month's total
//     limit-minutes budget, across monitored apps.
//   - "weeks saved" = all-time total saved minutes, expressed as an
//     equivalent number of full 7-day weeks.
//   - "hours/year projected" = average daily saved minutes so far,
//     annualized.
// Flag this to the team before treating the numbers as real — same category
// as the App Selection default daily limit.
// ---------------------------------------------------------------------------

data class TimeSavedSummary(
    val percentSavedThisMonth: Int,
    val daysActive: Int,
    val weeksSaved: Double,
    val projectedHoursPerYear: Int
)

fun calculateTimeSaved(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now()
): TimeSavedSummary {
    val limits = settings.filter { it.isBlocked }.associate { it.packageName to it.dailyLimitMinutes }
    val daysActive = logs.map { it.date }.distinct().size

    if (limits.isEmpty() || logs.isEmpty()) {
        return TimeSavedSummary(
            percentSavedThisMonth = 0,
            daysActive = daysActive,
            weeksSaved = 0.0,
            projectedHoursPerYear = 0
        )
    }

    fun minutesSaved(log: UsageLog): Int {
        val limit = limits[log.packageName] ?: return 0
        return (limit - log.totalMinutes).coerceAtLeast(0)
    }

    val totalMinutesSavedAllTime = logs.sumOf(::minutesSaved)

    val monthPrefix = today.toString().substring(0, 7) // "yyyy-MM"
    val thisMonthLogs = logs.filter { it.date.startsWith(monthPrefix) }
    val monthMinutesSaved = thisMonthLogs.sumOf(::minutesSaved)
    val monthMinutesBudget = thisMonthLogs.sumOf { limits[it.packageName] ?: 0 }
    val percentSavedThisMonth = if (monthMinutesBudget == 0) {
        0
    } else {
        ((monthMinutesSaved.toDouble() / monthMinutesBudget) * 100).coerceIn(0.0, 100.0).roundToInt()
    }

    val weeksSaved = totalMinutesSavedAllTime / (7.0 * 24 * 60)

    val averageDailyMinutesSaved = if (daysActive > 0) {
        totalMinutesSavedAllTime.toDouble() / daysActive
    } else {
        0.0
    }
    val projectedHoursPerYear = ((averageDailyMinutesSaved * 365) / 60).roundToInt()

    return TimeSavedSummary(
        percentSavedThisMonth = percentSavedThisMonth,
        daysActive = daysActive,
        weeksSaved = weeksSaved,
        projectedHoursPerYear = projectedHoursPerYear
    )
}
