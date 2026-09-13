package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Isolado de propósito: nenhuma função aqui toca em Room, Hilt, Context ou
// Compose. Ela só recebe listas e devolve um número, o que a torna trivial de
// testar e de encaixar em qualquer ViewModel depois.
// ---------------------------------------------------------------------------

/**
 * Quantos dias seguidos, contando pra trás a partir de [today], o usuário
 * ficou dentro do limite diário de TODOS os apps monitorados.
 *
 * "Dentro do limite" = para cada app com `isBlocked = true` em [settings],
 * os minutos daquele dia são menores ou iguais ao `dailyLimitMinutes` dele.
 * Basta um app estourar num dia para a sequência parar ali.
 *
 * @param logs todas as linhas de usage_log disponíveis (qualquer ordem).
 * @param settings todas as linhas de user_settings; as não monitoradas são ignoradas.
 * @param today a data de referência — parametrizada para dar pra testar sem
 *        depender do relógio do aparelho.
 */
fun calculateStreak(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now()
): Int {
    // Só os apps realmente monitorados. Desligar o switch na tela de seleção
    // não apaga a linha do banco, só vira isBlocked = false — por isso o filtro.
    val limits = settings
        .filter { it.isBlocked }
        .associate { it.packageName to it.dailyLimitMinutes }

    // Sem app monitorado não existe limite para cumprir, logo não existe streak.
    if (limits.isEmpty()) return 0

    // Reorganiza os logs em: data -> (pacote -> minutos), para consultar
    // qualquer dia sem varrer a lista inteira de novo a cada volta do laço.
    val minutesByDate: Map<String, Map<String, Int>> = logs
        .groupBy { it.date }
        .mapValues { (_, dayLogs) -> dayLogs.associate { it.packageName to it.totalMinutes } }

    // Piso da contagem: o dia mais antigo com registro. Sem isso, dias
    // anteriores à instalação do app (que têm zero minutos) contariam como
    // "dentro do limite" e o streak cresceria para sempre.
    val firstLoggedDay = minutesByDate.keys.minOrNull()?.let(LocalDate::parse) ?: return 0

    var streak = 0
    var day = today

    while (!day.isBefore(firstLoggedDay)) {
        val minutesThisDay = minutesByDate[day.toString()].orEmpty()

        // Sem linha no log = zero minutos naquele app, o que está dentro do limite.
        val withinLimit = limits.all { (packageName, limitMinutes) ->
            (minutesThisDay[packageName] ?: 0) <= limitMinutes
        }

        if (!withinLimit) break

        streak++
        day = day.minusDays(1)
    }

    return streak
}

/**
 * Versão pronta para a conquista "Under limit all week": o usuário cumpriu
 * os limites nos últimos 7 dias?
 */
fun hasFullWeekUnderLimit(
    logs: List<UsageLog>,
    settings: List<UserSettings>,
    today: LocalDate = LocalDate.now()
): Boolean = calculateStreak(logs, settings, today) >= DAYS_IN_WEEK

const val DAYS_IN_WEEK = 7
