package com.hackwestx.bloomscrolling.ui.home

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackwestx.bloomscrolling.data.SettingsDao
import com.hackwestx.bloomscrolling.data.UsageDao
import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import com.hackwestx.bloomscrolling.gamification.TimeSavedSummary
import com.hackwestx.bloomscrolling.gamification.calculateTimeSaved
import com.hackwestx.bloomscrolling.util.daysAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

private const val HISTORY_DAYS = 90
private const val MAX_RECENT_ENTRIES = 4

data class RecentSavingEntry(
    val appName: String,
    val subtitle: String,
    val minutesSaved: Int
)

data class HomeUiState(
    val summary: TimeSavedSummary = TimeSavedSummary(0, 0, 0.0, 0),
    val recentEntries: List<RecentSavingEntry> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    usageDao: UsageDao,
    settingsDao: SettingsDao
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        usageDao.observeSince(daysAgo(HISTORY_DAYS)),
        settingsDao.observeAll()
    ) { logs, settings ->
        HomeUiState(
            summary = calculateTimeSaved(logs, settings),
            recentEntries = recentSavingEntries(logs, settings, context)
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

private fun recentSavingEntries(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    context: Context
): List<RecentSavingEntry> {
    val limits = settings.filter { it.isBlocked }.associate { it.packageName to it.dailyLimitMinutes }
    val today = LocalDate.now()

    return logs
        .mapNotNull { log ->
            val limit = limits[log.packageName] ?: return@mapNotNull null
            val saved = (limit - log.totalMinutes).coerceAtLeast(0)
            if (saved <= 0) return@mapNotNull null
            Triple(log, saved, LocalDate.parse(log.date))
        }
        .sortedWith(compareByDescending<Triple<UsageLog, Int, LocalDate>> { it.third }.thenByDescending { it.second })
        .take(MAX_RECENT_ENTRIES)
        .map { (log, saved, date) ->
            RecentSavingEntry(
                appName = resolveAppName(context, log.packageName),
                subtitle = relativeDayLabel(date, today),
                minutesSaved = saved
            )
        }
}

private fun relativeDayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    else -> date.toString()
}

private fun resolveAppName(context: Context, packageName: String): String =
    try {
        @Suppress("DEPRECATION")
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
