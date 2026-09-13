package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.StreakState
import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Isolado de propósito: nenhuma função aqui toca em Room, Hilt, Context ou
// Compose. Só recebe listas e devolve números, o que a torna trivial de testar
// e de encaixar em qualquer ViewModel.
//
// REGRA DO DIA (mudou): antes cada app era comparado com o limite DELE e
// bastava um estourar para o dia cair. Agora vale o TOTAL: soma-se o uso de
// todos os apps monitorados no dia e compara-se com a soma dos limites. Passar
// 10 minutos no Instagram e economizar 10 no TikTok agora se compensam.
// ---------------------------------------------------------------------------

const val DAYS_IN_WEEK = 7

/** Quantos revives o usuário pode ter guardados ao mesmo tempo. */
const val MAX_REVIVES = 1

/**
 * Retrato completo da streak: o número atual, o que um revive faria, e se há
 * uma oferta de revive em aberto. Uma chamada só para a tela não precisar
 * recalcular a mesma coisa três vezes.
 */
data class StreakResult(
    val currentStreak: Int,
    val reviveOffer: ReviveOffer? = null
)

/**
 * Oferta de revive em aberto: "o dia [brokenDate] quebrou sua sequência; se
 * você gastar seu revive, ela volta para [restoredStreak] dias".
 */
data class ReviveOffer(
    val brokenDate: LocalDate,
    val restoredStreak: Int
)

// ---------------------------------------------------------------------------
// Cálculo do dia
// ---------------------------------------------------------------------------

/** Apps realmente monitorados -> limite diário de cada um. */
private fun limitsOf(settings: List<UserSettings>): Map<String, Int> = settings
    // Desligar o switch na tela de seleção não apaga a linha do banco, só vira
    // isBlocked = false — por isso o filtro.
    .filter { it.isBlocked }
    .associate { it.packageName to it.dailyLimitMinutes }

/** Logs reorganizados em: data -> (pacote -> minutos). */
private fun minutesByDateOf(logs: List<UsageLog>): Map<String, Map<String, Int>> = logs
    .groupBy { it.date }
    .mapValues { (_, dayLogs) -> dayLogs.associate { it.packageName to it.totalMinutes } }

/**
 * Minutos economizados num dia: soma dos limites menos soma do uso, só dos
 * apps monitorados.
 *
 * Pode ser NEGATIVO de propósito — é isso que marca o dia como perdido.
 * (Atenção: o `minutesSaved` de TimeSaved.kt é outra conta: é por app e é
 * travado em zero, então lá nunca fica negativo.)
 */
fun minutesSavedOnDay(
    minutesByDate: Map<String, Map<String, Int>>,
    limits: Map<String, Int>,
    day: LocalDate
): Int {
    val minutesThisDay = minutesByDate[day.toString()].orEmpty()
    // Sem linha no log = zero minutos naquele app.
    val used = limits.keys.sumOf { minutesThisDay[it] ?: 0 }
    return limits.values.sum() - used
}

/** O dia foi cumprido? Um dia perdoado por revive conta como cumprido. */
private fun dayHeld(
    minutesByDate: Map<String, Map<String, Int>>,
    limits: Map<String, Int>,
    day: LocalDate,
    revivedDates: Set<LocalDate>
): Boolean = day in revivedDates || minutesSavedOnDay(minutesByDate, limits, day) >= 0

/** Versão pública para as telas (strip da semana, etc.). */
fun dayHeldLimit(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    day: LocalDate,
    state: StreakState = StreakState()
): Boolean {
    val limits = limitsOf(settings)
    if (limits.isEmpty()) return false
    return dayHeld(minutesByDateOf(logs), limits, day, state.revivedDateSet())
}

// ---------------------------------------------------------------------------
// Streak
// ---------------------------------------------------------------------------

/**
 * Quantos dias seguidos, contando pra trás a partir de [today], o usuário
 * ficou dentro do limite total.
 *
 * [today] continua sendo o 3º parâmetro para não quebrar as chamadas
 * posicionais que já existem no projeto (ex.: StreakInsights).
 */
fun calculateStreak(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now(),
    state: StreakState = StreakState()
): Int = streakEndingAt(
    minutesByDate = minutesByDateOf(logs),
    limits = limitsOf(settings),
    today = today,
    revivedDates = state.revivedDateSet()
).length

/**
 * Streak + oferta de revive numa tacada só. É o que a tela de Streak usa.
 */
fun calculateStreakResult(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now(),
    state: StreakState = StreakState()
): StreakResult {
    val limits = limitsOf(settings)
    val minutesByDate = minutesByDateOf(logs)
    val revived = state.revivedDateSet()

    val run = streakEndingAt(minutesByDate, limits, today, revived)

    val offer = buildReviveOffer(
        minutesByDate = minutesByDate,
        limits = limits,
        today = today,
        revived = revived,
        brokenDate = run.brokenAt,
        revivesAvailable = state.revivesAvailable
    )

    return StreakResult(currentStreak = run.length, reviveOffer = offer)
}

/** Resultado interno: tamanho da sequência e onde ela parou (se parou). */
private data class StreakRun(val length: Int, val brokenAt: LocalDate?)

private fun streakEndingAt(
    minutesByDate: Map<String, Map<String, Int>>,
    limits: Map<String, Int>,
    today: LocalDate,
    revivedDates: Set<LocalDate>
): StreakRun {
    // Sem app monitorado não existe limite para cumprir, logo não existe streak.
    if (limits.isEmpty()) return StreakRun(0, null)

    // Piso da contagem: o dia mais antigo com registro. Sem isso, dias
    // anteriores à instalação (que têm zero minutos) contariam como cumpridos
    // e a streak cresceria para sempre.
    val firstLoggedDay = minutesByDate.keys.minOrNull()?.let(LocalDate::parse)
        ?: return StreakRun(0, null)

    var length = 0
    var day = today

    while (!day.isBefore(firstLoggedDay)) {
        if (!dayHeld(minutesByDate, limits, day, revivedDates)) {
            return StreakRun(length, day)
        }
        length++
        day = day.minusDays(1)
    }

    // Saiu pelo fim do histórico, não por um dia perdido: não há o que reviver.
    return StreakRun(length, null)
}

private fun buildReviveOffer(
    minutesByDate: Map<String, Map<String, Int>>,
    limits: Map<String, Int>,
    today: LocalDate,
    revived: Set<LocalDate>,
    brokenDate: LocalDate?,
    revivesAvailable: Int
): ReviveOffer? {
    if (revivesAvailable <= 0) return null
    if (brokenDate == null) return null

    // Hoje ainda não acabou: gastar o revive agora seria desperdício, porque a
    // pessoa ainda pode fechar o dia dentro do limite (ou não piorar mais).
    if (!brokenDate.isBefore(today)) return null

    // Quanto a streak voltaria a ser se este dia fosse perdoado.
    val restored = streakEndingAt(minutesByDate, limits, today, revived + brokenDate).length

    return ReviveOffer(brokenDate = brokenDate, restoredStreak = restored)
}

// ---------------------------------------------------------------------------
// Estado do revive (funções puras: recebem o estado, devolvem o novo)
// ---------------------------------------------------------------------------

/** Datas perdoadas, do CSV do banco para um Set de datas. */
fun StreakState.revivedDateSet(): Set<LocalDate> = revivedDates
    .split(',')
    .filter { it.isNotBlank() }
    .mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
    .toSet()

/**
 * Gasta o revive perdoando [brokenDate]. Devolve o novo estado — quem chama
 * é que grava no banco.
 */
fun useRevive(state: StreakState, brokenDate: LocalDate): StreakState {
    if (state.revivesAvailable <= 0) return state

    val dates = state.revivedDateSet() + brokenDate
    return state.copy(
        revivesAvailable = state.revivesAvailable - 1,
        revivedDates = dates.sorted().joinToString(",")
    )
}

/**
 * Dá um revive novo a cada marco de 7 dias de streak (7, 14, 21...).
 *
 * [StreakState.milestonesRewarded] guarda quantos marcos já foram pagos, senão
 * o mesmo marco premiaria de novo toda vez que a tela recalculasse. O total
 * nunca passa de [MAX_REVIVES].
 */
fun grantMilestoneRevives(state: StreakState, currentStreak: Int): StreakState {
    val milestonesReached = currentStreak / DAYS_IN_WEEK
    if (milestonesReached <= state.milestonesRewarded) return state

    val newlyEarned = milestonesReached - state.milestonesRewarded
    return state.copy(
        revivesAvailable = (state.revivesAvailable + newlyEarned).coerceAtMost(MAX_REVIVES),
        milestonesRewarded = milestonesReached
    )
}

/**
 * Versão pronta para a conquista "Under limit all week".
 */
fun hasFullWeekUnderLimit(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now(),
    state: StreakState = StreakState()
): Boolean = calculateStreak(logs, settings, today, state) >= DAYS_IN_WEEK
