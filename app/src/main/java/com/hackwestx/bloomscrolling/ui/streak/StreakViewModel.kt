package com.hackwestx.bloomscrolling.ui.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackwestx.bloomscrolling.data.SettingsDao
import com.hackwestx.bloomscrolling.data.SurveyDao
import com.hackwestx.bloomscrolling.data.UsageDao
import com.hackwestx.bloomscrolling.gamification.Achievement
import com.hackwestx.bloomscrolling.gamification.AchievementProgress
import com.hackwestx.bloomscrolling.gamification.DayStatus
import com.hackwestx.bloomscrolling.gamification.calculateLongestStreak
import com.hackwestx.bloomscrolling.gamification.calculateStreak
import com.hackwestx.bloomscrolling.gamification.countAnsweredSurveys
import com.hackwestx.bloomscrolling.gamification.evaluateAchievements
import com.hackwestx.bloomscrolling.gamification.lastWeekDayStatuses
import com.hackwestx.bloomscrolling.util.daysAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Practical bound, not "since the app was installed" — a schema query for
// the true earliest date would need a MIN(date) query that doesn't exist
// yet. 90 days is far more history than a 24-hour hackathon will ever log.
private const val HISTORY_DAYS = 90

data class StreakUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weekStatuses: List<DayStatus> = emptyList(),
    val achievements: Map<Achievement, Boolean> = emptyMap()
)

@HiltViewModel
class StreakViewModel @Inject constructor(
    usageDao: UsageDao,
    settingsDao: SettingsDao,
    surveyDao: SurveyDao
) : ViewModel() {

    val uiState: StateFlow<StreakUiState> = combine(
        usageDao.observeSince(daysAgo(HISTORY_DAYS)),
        settingsDao.observeAll(),
        surveyDao.observeAll()
    ) { logs, settings, surveys ->
        val currentStreak = calculateStreak(logs, settings)
        val progress = AchievementProgress(
            currentStreak = currentStreak,
            answeredSurveys = countAnsweredSurveys(surveys)
        )
        StreakUiState(
            currentStreak = currentStreak,
            longestStreak = calculateLongestStreak(logs, settings),
            weekStatuses = lastWeekDayStatuses(logs, settings),
            achievements = evaluateAchievements(progress)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakUiState())
}
