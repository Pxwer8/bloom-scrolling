package com.hackwestx.bloomscrolling.util

import java.time.LocalDate

// UsageLog.date is a device-local "yyyy-MM-dd" string (not epoch millis) and
// is part of its composite primary key — always write/query it via this
// helper so everyone stays on the same format.
fun startOfToday(): String = LocalDate.now().toString()

// Same yyyy-MM-dd format, n days before today — use as the lower bound for
// range queries (e.g. UsageDao.observeDailyTotals) instead of hand-rolling
// date math elsewhere.
fun daysAgo(n: Int): String = LocalDate.now().minusDays(n.toLong()).toString()
