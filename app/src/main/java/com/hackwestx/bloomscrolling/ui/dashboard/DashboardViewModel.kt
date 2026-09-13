package com.hackwestx.bloomscrolling.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackwestx.bloomscrolling.data.DailyUsageTotal
import com.hackwestx.bloomscrolling.data.ReasonCount
import com.hackwestx.bloomscrolling.data.SurveyDao
import com.hackwestx.bloomscrolling.data.UsageDao
import com.hackwestx.bloomscrolling.util.daysAgo
import com.hackwestx.bloomscrolling.util.startOfToday
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TREND_DAYS = 7

data class DashboardUiState(
    val todayTotalMinutes: Int = 0,
    val todayPickups: Int = 0,
    val reasonBreakdown: List<ReasonCount> = emptyList(),
    // Chart-library-agnostic on purpose: whichever chart ends up on top of
    // this (Vico or otherwise) reads plain (date, totalMinutes) pairs, so
    // swapping the chart later doesn't touch this ViewModel.
    val weeklyTrend: List<DailyUsageTotal> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageDao: UsageDao,
    private val surveyDao: SurveyDao
) : ViewModel() {

    // reasonBreakdown() is a one-shot suspend query, not a Flow, so it's
    // refreshed manually rather than live-updating like the usage data.
    private val reasonBreakdown = MutableStateFlow<List<ReasonCount>>(emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(
        usageDao.observeForDate(startOfToday()),
        usageDao.observeDailyTotals(daysAgo(TREND_DAYS - 1)),
        reasonBreakdown
    ) { todayLogs, weeklyTrend, breakdown ->
        DashboardUiState(
            todayTotalMinutes = todayLogs.sumOf { it.totalMinutes },
            todayPickups = todayLogs.sumOf { it.pickupCount },
            reasonBreakdown = breakdown,
            weeklyTrend = weeklyTrend
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        refreshReasonBreakdown()
    }

    fun refreshReasonBreakdown() {
        viewModelScope.launch {
            reasonBreakdown.value = surveyDao.reasonBreakdown()
        }
    }
}
