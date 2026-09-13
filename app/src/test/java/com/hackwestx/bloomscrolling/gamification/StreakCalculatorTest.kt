package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Teste JVM puro: sem Robolectric, sem emulador, sem banco. calculateStreak é
// uma função pura (listas entram, número sai), então dá pra testar direto —
// roda em milissegundos.
//
// A data de "hoje" é FIXA. Se usássemos LocalDate.now(), o teste passaria hoje
// e poderia falhar na virada do mês ou no horário de verão — o pior tipo de
// teste, o que quebra sozinho sem ninguém ter mexido no código.
// ---------------------------------------------------------------------------

private val TODAY = LocalDate.of(2026, 9, 12)

private const val INSTAGRAM = "com.instagram.android"
private const val TIKTOK = "com.zhiliaoapp.musically"
private const val LIMIT_MINUTES = 30

/** Data "yyyy-MM-dd" de N dias atrás; 0 = hoje. */
private fun day(daysAgo: Int): String = TODAY.minusDays(daysAgo.toLong()).toString()

private fun usage(daysAgo: Int, minutes: Int, packageName: String = INSTAGRAM) =
    UsageLog(
        date = day(daysAgo),
        packageName = packageName,
        totalMinutes = minutes,
        pickupCount = 1
    )

private fun monitored(packageName: String = INSTAGRAM, limit: Int = LIMIT_MINUTES) =
    UserSettings(packageName = packageName, dailyLimitMinutes = limit, isBlocked = true)

class StreakCalculatorTest {

    // --- Cenário 1 -----------------------------------------------------------

    @Test
    fun `three days in a row under the limit counts as a streak of three`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 10),
            usage(daysAgo = 1, minutes = 25),
            usage(daysAgo = 2, minutes = 5)
        )

        val streak = calculateStreak(logs, listOf(monitored()), today = TODAY)

        assertEquals(3, streak)
    }

    // --- Cenário 2 -----------------------------------------------------------

    /**
     * Hoje está dentro do limite, ontem estourou.
     *
     * O resultado correto é 1, não 0: "streak" é a quantidade de dias seguidos
     * contando pra trás a partir de hoje, e hoje conta. Zerar apagaria o dia
     * bom que o usuário está tendo agora — desmotivador e, além disso, errado:
     * a sequência atual realmente tem um dia de comprimento.
     */
    @Test
    fun `going over the limit yesterday cuts the streak down to today only`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 12),   // hoje: dentro
            usage(daysAgo = 1, minutes = 95),   // ontem: estourou
            usage(daysAgo = 2, minutes = 8)     // anteontem: dentro, mas já não alcança
        )

        val streak = calculateStreak(logs, listOf(monitored()), today = TODAY)

        assertEquals(1, streak)
    }

    @Test
    fun `going over the limit today gives a streak of zero`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 60),
            usage(daysAgo = 1, minutes = 10)
        )

        val streak = calculateStreak(logs, listOf(monitored()), today = TODAY)

        assertEquals(0, streak)
    }

    // --- Cenário 3 -----------------------------------------------------------

    @Test
    fun `no usage data at all gives a streak of zero`() {
        val streak = calculateStreak(
            logs = emptyList(),
            settings = listOf(monitored()),
            today = TODAY
        )

        assertEquals(0, streak)
    }

    // --- Casos de borda ------------------------------------------------------

    @Test
    fun `no monitored app gives a streak of zero even with clean usage`() {
        val logs = listOf(usage(daysAgo = 0, minutes = 1), usage(daysAgo = 1, minutes = 1))
        val notMonitored = UserSettings(INSTAGRAM, LIMIT_MINUTES, isBlocked = false)

        val streak = calculateStreak(logs, listOf(notMonitored), today = TODAY)

        assertEquals(0, streak)
    }

    @Test
    fun `one monitored app over the limit breaks the day for all of them`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 5, packageName = INSTAGRAM),
            usage(daysAgo = 0, minutes = 90, packageName = TIKTOK),
            usage(daysAgo = 1, minutes = 5, packageName = INSTAGRAM),
            usage(daysAgo = 1, minutes = 5, packageName = TIKTOK)
        )

        val streak = calculateStreak(
            logs,
            listOf(monitored(INSTAGRAM), monitored(TIKTOK)),
            today = TODAY
        )

        assertEquals(0, streak)
    }

    @Test
    fun `an unmonitored app over the limit does not break the streak`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 5, packageName = INSTAGRAM),
            usage(daysAgo = 0, minutes = 300, packageName = TIKTOK)
        )

        val streak = calculateStreak(logs, listOf(monitored(INSTAGRAM)), today = TODAY)

        assertEquals(1, streak)
    }

    @Test
    fun `usage exactly at the limit still counts as within the limit`() {
        val logs = listOf(usage(daysAgo = 0, minutes = LIMIT_MINUTES))

        val streak = calculateStreak(logs, listOf(monitored()), today = TODAY)

        assertEquals(1, streak)
    }

    @Test
    fun `a day with no log row counts as zero minutes and keeps the streak`() {
        // Nada registrado ontem (o usuário nem abriu o app) — isso é
        // cumprir o limite, não uma lacuna que quebra a sequência.
        val logs = listOf(
            usage(daysAgo = 0, minutes = 3),
            usage(daysAgo = 2, minutes = 3)
        )

        val streak = calculateStreak(logs, listOf(monitored()), today = TODAY)

        assertEquals(3, streak)
    }

    @Test
    fun `the streak never counts days before the first logged day`() {
        // Só um dia de histórico: o streak não pode passar de 1, mesmo que
        // todos os dias anteriores "tecnicamente" tenham zero minutos.
        val logs = listOf(usage(daysAgo = 0, minutes = 2))

        val streak = calculateStreak(logs, listOf(monitored()), today = TODAY)

        assertEquals(1, streak)
    }

    @Test
    fun `a full week under the limit unlocks the weekly achievement`() {
        val logs = (0..6).map { usage(daysAgo = it, minutes = 10) }

        assertEquals(7, calculateStreak(logs, listOf(monitored()), today = TODAY))
        assertEquals(true, hasFullWeekUnderLimit(logs, listOf(monitored()), today = TODAY))
    }
}
