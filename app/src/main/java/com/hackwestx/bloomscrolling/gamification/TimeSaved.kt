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
//     limit-minutes budget, across monitored apps and across every DAY of the
//     month so far (not only days with a usage_log row).
//   - "weeks saved" = all-time total saved minutes, expressed in "waking
//     weeks" ([WAKING_MINUTES_PER_WEEK]), not 168-hour calendar weeks.
//   - "hours/year projected" = average daily saved minutes so far,
//     annualized.
// Flag this to the team before treating the numbers as real — same category
// as the App Selection default daily limit.
// ---------------------------------------------------------------------------

/**
 * Uma "semana de vigília": 16 horas por dia, 7 dias.
 *
 * Dividir por uma semana corrida (7 × 24 × 60 = 10.080) fazia o número ficar
 * praticamente sempre 0.0 na tela, porque ninguém economiza tempo de tela
 * enquanto dorme. Comparar com as horas acordado é a comparação que o usuário
 * de fato faz na cabeça.
 */
private const val WAKING_MINUTES_PER_WEEK = 16 * 60 * 7 // 6.720

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

    // Só conta como "dia ativo" o dia em que houve uso de um app MONITORADO.
    // Antes contava qualquer linha do usage_log, então um app que o usuário
    // nem pediu para acompanhar inflava o número.
    val daysActive = logs
        .filter { it.packageName in limits }
        .map { it.date }
        .distinct()
        .size

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

    val percentSavedThisMonth = percentSavedInMonth(logs, limits, today)

    val weeksSaved = totalMinutesSavedAllTime.toDouble() / WAKING_MINUTES_PER_WEEK

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

/**
 * Porcentagem economizada no mês corrente.
 *
 * Percorre os DIAS do calendário, não as linhas do usage_log. Essa é a
 * correção principal: um dia em que o usuário não abriu nenhum app monitorado
 * não gera linha no banco, e antes esse dia sumia das duas pontas da conta
 * (numerador e denominador) — ou seja, o dia perfeito não melhorava nada.
 * Agora ele entra com economia igual ao limite inteiro, que é o que ele é.
 *
 * O intervalo vai do dia 1 do mês (ou do primeiro dia com registro, o que for
 * mais recente) até hoje. O piso pelo primeiro registro evita creditar dias
 * anteriores à instalação do app; parar em hoje evita creditar o futuro.
 */
private fun percentSavedInMonth(
    logs: List<UsageLog>,
    limits: Map<String, Int>,
    today: LocalDate
): Int {
    val minutesByDate: Map<String, Map<String, Int>> = logs
        .groupBy { it.date }
        .mapValues { (_, dayLogs) -> dayLogs.associate { it.packageName to it.totalMinutes } }

    val firstLoggedDay = minutesByDate.keys.minOrNull()?.let(LocalDate::parse) ?: return 0
    val rangeStart = maxOf(today.withDayOfMonth(1), firstLoggedDay)
    if (rangeStart.isAfter(today)) return 0

    var savedMinutes = 0
    var budgetMinutes = 0
    var day = rangeStart

    while (!day.isAfter(today)) {
        val minutesThisDay = minutesByDate[day.toString()].orEmpty()
        limits.forEach { (packageName, limit) ->
            budgetMinutes += limit
            // Sem linha no log = zero minutos usados = economizou o limite todo.
            savedMinutes += (limit - (minutesThisDay[packageName] ?: 0)).coerceAtLeast(0)
        }
        day = day.plusDays(1)
    }

    if (budgetMinutes == 0) return 0

    return ((savedMinutes.toDouble() / budgetMinutes) * 100)
        .coerceIn(0.0, 100.0)
        .roundToInt()
}
