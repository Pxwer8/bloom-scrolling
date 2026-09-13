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
//   - every stat below walks CALENDAR DAYS, never usage_log rows, so a day
//     with no usage at all counts as saving the whole limit instead of
//     disappearing from the maths (see [scanDays]).
//   - "% saved this month" = saved / budget over the days of this month.
//   - "weeks saved" = all-time saved minutes in "waking weeks"
//     ([WAKING_MINUTES_PER_WEEK]), not 168-hour calendar weeks.
//   - "hours/year projected" = average saved minutes per day over the whole
//     tracked period, annualized.
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
    //
    // Este número é só para exibição. Ele NÃO é mais o divisor da média
    // diária — ver [projectedHoursPerYear] abaixo.
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

    val minutesByDate: Map<String, Map<String, Int>> = logs
        .groupBy { it.date }
        .mapValues { (_, dayLogs) -> dayLogs.associate { it.packageName to it.totalMinutes } }

    // Piso de todo cálculo: o primeiro dia com registro. Sem ele, dias
    // anteriores à instalação do app (que não têm uso nenhum) entrariam como
    // economia grátis e todos os números explodiriam.
    val firstLoggedDay = minutesByDate.keys.minOrNull()?.let(LocalDate::parse)
        ?: return TimeSavedSummary(0, daysActive, 0.0, 0)

    // Período inteiro acompanhado: do primeiro registro até hoje.
    val allTime = scanDays(minutesByDate, limits, from = firstLoggedDay, to = today)

    // Mês corrente: do dia 1 (ou do primeiro registro, se for mais recente)
    // até hoje. Mesmo piso, janela menor.
    val thisMonth = scanDays(
        minutesByDate,
        limits,
        from = maxOf(today.withDayOfMonth(1), firstLoggedDay),
        to = today
    )

    val weeksSaved = allTime.savedMinutes.toDouble() / WAKING_MINUTES_PER_WEEK

    // Média sobre TODOS os dias do período, não só sobre os dias com uso
    // registrado. Se o numerador passou a incluir os dias perfeitos, o
    // denominador precisa incluí-los também — senão a média fica acima do
    // próprio limite diário (ex.: 10 dias economizando 30 min divididos por
    // 1 "dia ativo" dariam 300 min/dia).
    val averageDailyMinutesSaved = if (allTime.dayCount > 0) {
        allTime.savedMinutes.toDouble() / allTime.dayCount
    } else {
        0.0
    }

    return TimeSavedSummary(
        percentSavedThisMonth = thisMonth.percent(),
        daysActive = daysActive,
        weeksSaved = weeksSaved,
        projectedHoursPerYear = ((averageDailyMinutesSaved * 365) / 60).roundToInt()
    )
}

/** Economia, orçamento e quantidade de dias de um intervalo. */
private data class SavedOverRange(
    val savedMinutes: Int,
    val budgetMinutes: Int,
    val dayCount: Int
) {
    fun percent(): Int {
        if (budgetMinutes == 0) return 0
        return ((savedMinutes.toDouble() / budgetMinutes) * 100)
            .coerceIn(0.0, 100.0)
            .roundToInt()
    }
}

/**
 * Varre DIA A DIA do calendário, de [from] até [to] inclusive, somando quanto
 * foi economizado e quanto era o orçamento.
 *
 * Esta é a peça central das correções: iterar sobre dias em vez de sobre as
 * linhas do usage_log. Um dia em que o usuário não abriu nenhum app monitorado
 * simplesmente não tem linha no banco — iterando linhas, esse dia sumia da
 * conta inteira em vez de contar como economia total, que é o que ele é.
 */
private fun scanDays(
    minutesByDate: Map<String, Map<String, Int>>,
    limits: Map<String, Int>,
    from: LocalDate,
    to: LocalDate
): SavedOverRange {
    if (from.isAfter(to)) return SavedOverRange(0, 0, 0)

    var savedMinutes = 0
    var budgetMinutes = 0
    var dayCount = 0
    var day = from

    while (!day.isAfter(to)) {
        val minutesThisDay = minutesByDate[day.toString()].orEmpty()
        limits.forEach { (packageName, limit) ->
            budgetMinutes += limit
            // Sem linha no log = zero minutos usados = economizou o limite todo.
            savedMinutes += (limit - (minutesThisDay[packageName] ?: 0)).coerceAtLeast(0)
        }
        dayCount++
        day = day.plusDays(1)
    }

    return SavedOverRange(savedMinutes, budgetMinutes, dayCount)
}
