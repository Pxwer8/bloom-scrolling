package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 9, 12) // 2026-09-12
private val FIRST_OF_MONTH = LocalDate.of(2026, 9, 1)

private const val INSTAGRAM = "com.instagram.android"
private const val TIKTOK = "com.zhiliaoapp.musically"

private fun monitored(packageName: String = INSTAGRAM, limit: Int) =
    UserSettings(packageName = packageName, dailyLimitMinutes = limit, isBlocked = true)

class TimeSavedTest {

    @Test
    fun `no monitored apps means nothing to save`() {
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-12", INSTAGRAM, totalMinutes = 5, pickupCount = 1)),
            settings = emptyList(),
            today = TODAY
        )
        assertEquals(0, summary.percentSavedThisMonth)
        assertEquals(0.0, summary.weeksSaved, 0.0)
        assertEquals(0, summary.projectedHoursPerYear)
    }

    // -----------------------------------------------------------------------
    // percentSavedThisMonth
    // -----------------------------------------------------------------------

    @Test
    fun `going over the limit saves nothing, never negative`() {
        // Único dia do mês (hoje = dia 1), estourado: 0% e nunca negativo.
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 90, pickupCount = 1)),
            settings = listOf(monitored(limit = 30)),
            today = FIRST_OF_MONTH
        )
        assertEquals(0, summary.percentSavedThisMonth)
        assertEquals(1, summary.daysActive)
    }

    @Test
    fun `percent saved this month is the ratio of saved minutes to the monthly limit budget`() {
        // Um só dia no mês: limite 30, usou 10 -> economizou 20 de 30 = ~67%.
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 10, pickupCount = 1)),
            settings = listOf(monitored(limit = 30)),
            today = FIRST_OF_MONTH
        )
        assertEquals(67, summary.percentSavedThisMonth)
    }

    @Test
    fun `a day with no usage row counts as saving the whole limit`() {
        // CORREÇÃO PRINCIPAL: dias 1 e 2 registrados no limite cheio (0%),
        // dia 3 sem nenhuma linha. Antes o dia 3 sumia da conta e o resultado
        // era 0%; agora ele entra como economia total.
        // Orçamento: 3 dias x 30 = 90. Economia: 0 + 0 + 30 = 30 -> 33%.
        val summary = calculateTimeSaved(
            logs = listOf(
                UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 30, pickupCount = 1),
                UsageLog("2026-09-02", INSTAGRAM, totalMinutes = 30, pickupCount = 1)
            ),
            settings = listOf(monitored(limit = 30)),
            today = LocalDate.of(2026, 9, 3)
        )
        assertEquals(33, summary.percentSavedThisMonth)
    }

    @Test
    fun `a perfect day improves the percentage instead of being ignored`() {
        val settings = listOf(monitored(limit = 30))
        val usedDay = UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 30, pickupCount = 1)

        // Só o dia gasto: 0%.
        val onlyUsedDay = calculateTimeSaved(listOf(usedDay), settings, FIRST_OF_MONTH)
        // Mesmo histórico, mas o mês já tem um segundo dia — sem uso nenhum.
        val plusPerfectDay = calculateTimeSaved(listOf(usedDay), settings, FIRST_OF_MONTH.plusDays(1))

        assertEquals(0, onlyUsedDay.percentSavedThisMonth)
        assertEquals(50, plusPerfectDay.percentSavedThisMonth)
        assertTrue(plusPerfectDay.percentSavedThisMonth > onlyUsedDay.percentSavedThisMonth)
    }

    @Test
    fun `days before the first logged day are not credited`() {
        // Primeiro registro é dia 10, então os dias 1-9 do mês (anteriores ao
        // uso do app) não podem entrar como economia grátis.
        // Orçamento: dias 10, 11 e 12 = 3 x 30 = 90. Economia: 20 + 30 + 30 = 80 -> 89%.
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-10", INSTAGRAM, totalMinutes = 10, pickupCount = 1)),
            settings = listOf(monitored(limit = 30)),
            today = TODAY
        )
        assertEquals(89, summary.percentSavedThisMonth)
    }

    @Test
    fun `days from a different month do not count toward this month's percentage`() {
        val summary = calculateTimeSaved(
            logs = listOf(
                // Mês passado, economia total — não pode influenciar setembro.
                UsageLog("2026-08-15", INSTAGRAM, totalMinutes = 0, pickupCount = 0),
                // Este mês, no limite cheio: 0 economizado no único dia do mês.
                UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 30, pickupCount = 1)
            ),
            settings = listOf(monitored(limit = 30)),
            today = FIRST_OF_MONTH
        )
        assertEquals(0, summary.percentSavedThisMonth)
        // Mas os dois dias continuam contando para as estatísticas gerais.
        assertEquals(2, summary.daysActive)
    }

    // -----------------------------------------------------------------------
    // daysActive
    // -----------------------------------------------------------------------

    @Test
    fun `days active only counts days with activity in a monitored app`() {
        val summary = calculateTimeSaved(
            logs = listOf(
                UsageLog("2026-09-10", INSTAGRAM, totalMinutes = 5, pickupCount = 1),
                // TikTok não está monitorado: este dia não deve contar.
                UsageLog("2026-09-11", TIKTOK, totalMinutes = 200, pickupCount = 9)
            ),
            settings = listOf(monitored(INSTAGRAM, limit = 30)),
            today = TODAY
        )
        assertEquals(1, summary.daysActive)
    }

    @Test
    fun `a day with both a monitored and an unmonitored app still counts once`() {
        val summary = calculateTimeSaved(
            logs = listOf(
                UsageLog("2026-09-10", INSTAGRAM, totalMinutes = 5, pickupCount = 1),
                UsageLog("2026-09-10", TIKTOK, totalMinutes = 200, pickupCount = 9)
            ),
            settings = listOf(monitored(INSTAGRAM, limit = 30)),
            today = TODAY
        )
        assertEquals(1, summary.daysActive)
    }

    // -----------------------------------------------------------------------
    // weeksSaved
    // -----------------------------------------------------------------------

    @Test
    fun `weeks saved converts all-time saved minutes into waking-week units`() {
        // 7 dias economizando 16h (960 min) cada = 6.720 min = uma semana
        // de vigília inteira.
        val logs = (0..6).map {
            UsageLog(TODAY.minusDays(it.toLong()).toString(), INSTAGRAM, totalMinutes = 0, pickupCount = 0)
        }
        val summary = calculateTimeSaved(logs, listOf(monitored(limit = 960)), TODAY)
        assertEquals(1.0, summary.weeksSaved, 0.001)
    }

    @Test
    fun `a perfect day improves weeks saved instead of being ignored`() {
        val settings = listOf(monitored(limit = 30))
        // Um único dia registrado, gasto até o limite: economia zero.
        val usedDay = listOf(UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 30, pickupCount = 1))

        val onlyUsedDay = calculateTimeSaved(usedDay, settings, FIRST_OF_MONTH)
        // Mesmo histórico, mas o período já tem um segundo dia — sem uso nenhum.
        val plusPerfectDay = calculateTimeSaved(usedDay, settings, FIRST_OF_MONTH.plusDays(1))

        assertEquals(0.0, onlyUsedDay.weeksSaved, 0.0)
        assertEquals(30.0 / 6720, plusPerfectDay.weeksSaved, 0.0001)
        assertTrue(plusPerfectDay.weeksSaved > onlyUsedDay.weeksSaved)
    }

    @Test
    fun `a perfect day improves the projected hours per year instead of being ignored`() {
        val settings = listOf(monitored(limit = 30))
        val usedDay = listOf(UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 30, pickupCount = 1))

        val onlyUsedDay = calculateTimeSaved(usedDay, settings, FIRST_OF_MONTH)
        val plusPerfectDay = calculateTimeSaved(usedDay, settings, FIRST_OF_MONTH.plusDays(1))

        // 1 dia, economia 0 -> média 0/dia -> 0 h/ano.
        assertEquals(0, onlyUsedDay.projectedHoursPerYear)
        // 2 dias, economia 30 -> média 15 min/dia -> 15 * 365 / 60 = 91 h/ano.
        assertEquals(91, plusPerfectDay.projectedHoursPerYear)
    }

    @Test
    fun `the projection averages over every tracked day, not only the active ones`() {
        // 1 dia de uso no limite cheio + 9 dias perfeitos.
        // Economia: 0 + 9 x 30 = 270 em 10 dias -> média 27 min/dia
        // -> 27 * 365 / 60 = 164 h/ano.
        // Se a média ainda dividisse por daysActive (= 1), daria 270 min/dia,
        // ou seja, nove vezes o próprio limite diário: absurdo.
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-01", INSTAGRAM, totalMinutes = 30, pickupCount = 1)),
            settings = listOf(monitored(limit = 30)),
            today = LocalDate.of(2026, 9, 10)
        )

        assertEquals(1, summary.daysActive)
        assertEquals(164, summary.projectedHoursPerYear)
    }

    @Test
    fun `all-time totals start at the first logged day, not before it`() {
        // Primeiro (e único) registro no dia 10, hoje é 12: o período são
        // 3 dias, não o mês inteiro nem desde sempre.
        // Economia: 3 x 30 = 90 minutos.
        val summary = calculateTimeSaved(
            logs = listOf(UsageLog("2026-09-10", INSTAGRAM, totalMinutes = 0, pickupCount = 0)),
            settings = listOf(monitored(limit = 30)),
            today = TODAY
        )

        assertEquals(90.0 / 6720, summary.weeksSaved, 0.0001)
        // Média 30 min/dia -> 30 * 365 / 60 = 182,5 -> 183 h/ano.
        assertEquals(183, summary.projectedHoursPerYear)
    }

    @Test
    fun `a realistic week of saving shows up as a visible fraction of a week`() {
        // 7 dias economizando 30 min cada = 210 min.
        // Divisor antigo (10.080): 0,02 -> aparecia como "0.0" na tela.
        // Divisor novo (6.720): 0,031 -> ainda pequeno, mas o dobro.
        val logs = (0..6).map {
            UsageLog(TODAY.minusDays(it.toLong()).toString(), INSTAGRAM, totalMinutes = 0, pickupCount = 0)
        }
        val summary = calculateTimeSaved(logs, listOf(monitored(limit = 30)), TODAY)
        assertEquals(210.0 / 6720, summary.weeksSaved, 0.0001)
    }
}
