package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.StreakState
import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Two more pure, isolated calculators in the same spirit as StreakCalculator
// (no Room/Hilt/Context/Compose) — kept in a separate file rather than
// editing StreakCalculator.kt directly, since that file isn't mine.
// ---------------------------------------------------------------------------

/**
 * The longest streak ever achieved, not just the current one. Implemented by
 * re-running [calculateStreak] with every logged day as the reference "today"
 * and taking the max — not the most efficient approach, but a hackathon's
 * worth of logs is tiny, and it guarantees identical semantics to
 * [calculateStreak] with no duplicated logic.
 */
fun calculateLongestStreak(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now(),
    state: StreakState = StreakState()
): Int {
    if (logs.isEmpty()) return 0
    val firstLoggedDay = logs.minOf { LocalDate.parse(it.date) }

    var longest = 0
    var day = firstLoggedDay
    while (!day.isAfter(today)) {
        val streakEndingHere = calculateStreak(logs, settings, day, state)
        if (streakEndingHere > longest) longest = streakEndingHere
        day = day.plusDays(1)
    }
    return longest
}

data class DayStatus(val date: LocalDate, val heldLimit: Boolean)

/**
 * Per-day pass/fail for the last 7 calendar days (oldest first), for a
 * week-strip UI. Unlike [calculateStreak] this doesn't stop at the first
 * broken day — every day gets its own independent status.
 */
fun lastWeekDayStatuses(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now(),
    state: StreakState = StreakState()
): List<DayStatus> = (6 downTo 0).map { offset ->
    val day = today.minusDays(offset.toLong())
    // Usa a MESMA regra do calculateStreak (total do dia vs. total dos
    // limites, com dias revividos contando como cumpridos). Se as duas contas
    // divergissem, a bolinha da semana contradiria o número da streak.
    DayStatus(day, dayHeldLimit(logs, settings, day, state))
}
