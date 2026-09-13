package com.hackwestx.bloomscrolling.ui.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackwestx.bloomscrolling.data.SettingsDao
import com.hackwestx.bloomscrolling.data.StreakState
import com.hackwestx.bloomscrolling.data.StreakStateDao
import com.hackwestx.bloomscrolling.data.SurveyDao
import com.hackwestx.bloomscrolling.data.UsageDao
import com.hackwestx.bloomscrolling.gamification.Achievement
import com.hackwestx.bloomscrolling.gamification.AchievementProgress
import com.hackwestx.bloomscrolling.gamification.DayStatus
import com.hackwestx.bloomscrolling.gamification.ReviveOffer
import com.hackwestx.bloomscrolling.gamification.calculateLongestStreak
import com.hackwestx.bloomscrolling.gamification.calculateStreakResult
import com.hackwestx.bloomscrolling.gamification.countAnsweredSurveys
import com.hackwestx.bloomscrolling.gamification.evaluateAchievements
import com.hackwestx.bloomscrolling.gamification.grantMilestoneRevives
import com.hackwestx.bloomscrolling.gamification.lastWeekDayStatuses
import com.hackwestx.bloomscrolling.gamification.useRevive
import com.hackwestx.bloomscrolling.util.daysAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Practical bound, not "since the app was installed" — a schema query for
// the true earliest date would need a MIN(date) query that doesn't exist
// yet. 90 days is far more history than a 24-hour hackathon will ever log.
private const val HISTORY_DAYS = 90

data class StreakUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weekStatuses: List<DayStatus> = emptyList(),
    val achievements: Map<Achievement, Boolean> = emptyMap(),
    /** Não-nulo quando há um dia perdido que pode ser revivido. */
    val reviveOffer: ReviveOffer? = null,
    val revivesAvailable: Int = 0
)

@HiltViewModel
class StreakViewModel @Inject constructor(
    usageDao: UsageDao,
    settingsDao: SettingsDao,
    surveyDao: SurveyDao,
    private val streakStateDao: StreakStateDao
) : ViewModel() {

    /**
     * A oferta some da tela quando o usuário escolhe "deixar zerar". É só de
     * memória: se ele sair e voltar, a oferta reaparece — o revive não foi
     * gasto, então guardar essa recusa no banco seria punir sem necessidade.
     */
    private val offerDismissed = MutableStateFlow(false)

    private val streakState: StateFlow<StreakState> = streakStateDao.observe()
        .map { it ?: StreakState() } // primeira execução: ainda não há linha
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakState())

    val uiState: StateFlow<StreakUiState> = combine(
        usageDao.observeSince(daysAgo(HISTORY_DAYS)),
        settingsDao.observeAll(),
        surveyDao.observeAll(),
        streakState,
        offerDismissed
    ) { logs, settings, surveys, state, dismissed ->
        val result = calculateStreakResult(logs, settings, state = state)

        // Marco de 7 dias alcançado => ganha um revive. Gravado aqui porque é
        // consequência automática da streak, não de uma ação do usuário.
        rewardMilestoneIfDue(state, result.currentStreak)

        val progress = AchievementProgress(
            currentStreak = result.currentStreak,
            answeredSurveys = countAnsweredSurveys(surveys)
        )

        StreakUiState(
            currentStreak = result.currentStreak,
            longestStreak = calculateLongestStreak(logs, settings, state = state),
            weekStatuses = lastWeekDayStatuses(logs, settings, state = state),
            achievements = evaluateAchievements(progress),
            reviveOffer = if (dismissed) null else result.reviveOffer,
            revivesAvailable = state.revivesAvailable
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakUiState())

    /** "Usar revive": perdoa o dia perdido e gasta o revive. */
    fun useReviveOn(offer: ReviveOffer) {
        viewModelScope.launch {
            val current = streakStateDao.get() ?: StreakState()
            streakStateDao.upsert(useRevive(current, offer.brokenDate))
        }
    }

    /** "Deixar zerar": só esconde a oferta nesta sessão, não gasta nada. */
    fun dismissReviveOffer() {
        offerDismissed.value = true
    }

    private fun rewardMilestoneIfDue(state: StreakState, currentStreak: Int) {
        val rewarded = grantMilestoneRevives(state, currentStreak)
        if (rewarded != state) {
            viewModelScope.launch { streakStateDao.upsert(rewarded) }
        }
    }
}
