package com.hackwestx.bloomscrolling.util

import java.time.LocalDate

// UsageLog.date is a device-local "yyyy-MM-dd" string (not epoch millis) and
// is part of its composite primary key — always write/query it via this
// helper so everyone stays on the same format.
fun startOfToday(): String = LocalDate.now().toString()
