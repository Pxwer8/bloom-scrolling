package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.StreakState
import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Teste JVM puro: sem Robolectric, sem emulador, sem banco.
//
// A data de "hoje" é FIXA. Se usássemos LocalDate.now(), o teste passaria hoje
// e poderia falhar na virada do mês — o pior tipo de teste, o que quebra
// sozinho sem ninguém ter mexido no código.
// ---------------------------------------------------------------------------

private val TODAY = LocalDate.of(2026, 9, 13)

private const val INSTAGRAM = "com.instagram.android"
private const val TIKTOK = "com.zhiliaoapp.musically"
private const val LIMIT_MINUTES = 30

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

/** Estado sem revive guardado (o usuário já gastou o dele). */
private val NO_REVIVES = StreakState(revivesAvailable = 0)

class StreakCalculatorTest {

    // -----------------------------------------------------------------------
    // Regra do dia: soma do uso vs. soma dos limites
    // -----------------------------------------------------------------------

    @Test
    fun `three days in a row under the limit counts as a streak of three`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 10),
            usage(daysAgo = 1, minutes = 25),
            usage(daysAgo = 2, minutes = 5)
        )

        assertEquals(3, calculateStreak(logs, listOf(monitored()), today = TODAY))
    }

    @Test
    fun `one app over budget is forgiven when another app is under by as much`() {
        // NOVA REGRA: 50 no Instagram + 5 no TikTok = 55, dentro dos 60 de
        // orçamento total (30 + 30). Na regra antiga isto quebraria o dia,
        // porque o Instagram sozinho estourou o limite dele.
        val logs = listOf(
            usage(daysAgo = 0, minutes = 50, packageName = INSTAGRAM),
            usage(daysAgo = 0, minutes = 5, packageName = TIKTOK)
        )

        val streak = calculateStreak(
            logs,
            listOf(monitored(INSTAGRAM), monitored(TIKTOK)),
            today = TODAY
        )

        assertEquals(1, streak)
    }

    @Test
    fun `the day is lost when the combined total goes over the combined budget`() {
        // 50 + 20 = 70, acima dos 60 de orçamento.
        val logs = listOf(
            usage(daysAgo = 0, minutes = 50, packageName = INSTAGRAM),
            usage(daysAgo = 0, minutes = 20, packageName = TIKTOK)
        )

        val streak = calculateStreak(
            logs,
            listOf(monitored(INSTAGRAM), monitored(TIKTOK)),
            today = TODAY
        )

        assertEquals(0, streak)
    }

    @Test
    fun `usage exactly at the limit still counts as within the limit`() {
        // Economia de exatamente 0 minutos ainda satisfaz "economizado >= 0".
        val logs = listOf(usage(daysAgo = 0, minutes = LIMIT_MINUTES))

        assertEquals(1, calculateStreak(logs, listOf(monitored()), today = TODAY))
    }

    @Test
    fun `an unmonitored app is ignored no matter how much it is used`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 5, packageName = INSTAGRAM),
            usage(daysAgo = 0, minutes = 300, packageName = TIKTOK)
        )

        assertEquals(1, calculateStreak(logs, listOf(monitored(INSTAGRAM)), today = TODAY))
    }

    @Test
    fun `minutes saved on a day can be negative and that is what breaks it`() {
        val logs = listOf(usage(daysAgo = 0, minutes = 45))
        val settings = listOf(monitored(limit = 30))

        val minutesByDate = mapOf(day(0) to mapOf(INSTAGRAM to 45))
        assertEquals(-15, minutesSavedOnDay(minutesByDate, mapOf(INSTAGRAM to 30), TODAY))
        assertEquals(0, calculateStreak(logs, settings, today = TODAY))
    }

    // -----------------------------------------------------------------------
    // Casos de borda herdados (continuam valendo na nova regra)
    // -----------------------------------------------------------------------

    @Test
    fun `no usage data at all gives a streak of zero`() {
        assertEquals(0, calculateStreak(emptyList(), listOf(monitored()), today = TODAY))
    }

    @Test
    fun `no monitored app gives a streak of zero even with clean usage`() {
        val logs = listOf(usage(daysAgo = 0, minutes = 1), usage(daysAgo = 1, minutes = 1))
        val notMonitored = UserSettings(INSTAGRAM, LIMIT_MINUTES, isBlocked = false)

        assertEquals(0, calculateStreak(logs, listOf(notMonitored), today = TODAY))
    }

    @Test
    fun `a day with no log row counts as zero minutes and keeps the streak`() {
        val logs = listOf(usage(daysAgo = 0, minutes = 3), usage(daysAgo = 2, minutes = 3))

        assertEquals(3, calculateStreak(logs, listOf(monitored()), today = TODAY))
    }

    @Test
    fun `the streak never counts days before the first logged day`() {
        val logs = listOf(usage(daysAgo = 0, minutes = 2))

        assertEquals(1, calculateStreak(logs, listOf(monitored()), today = TODAY))
    }

    @Test
    fun `a full week under the limit unlocks the weekly achievement`() {
        val logs = (0..6).map { usage(daysAgo = it, minutes = 10) }

        assertEquals(7, calculateStreak(logs, listOf(monitored()), today = TODAY))
        assertTrue(hasFullWeekUnderLimit(logs, listOf(monitored()), today = TODAY))
    }

    // -----------------------------------------------------------------------
    // Revive: a oferta
    // -----------------------------------------------------------------------

    @Test
    fun `going over yesterday offers a revive that restores the whole run`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 10),   // hoje: ok
            usage(daysAgo = 1, minutes = 95),   // ontem: estourou
            usage(daysAgo = 2, minutes = 5),    // antes disso, tudo ok
            usage(daysAgo = 3, minutes = 5),
            usage(daysAgo = 4, minutes = 5)
        )

        val result = calculateStreakResult(logs, listOf(monitored()), today = TODAY)

        // Sem usar o revive, a sequência é só o dia de hoje.
        assertEquals(1, result.currentStreak)

        val offer = result.reviveOffer
        assertNotNull(offer)
        assertEquals(TODAY.minusDays(1), offer!!.brokenDate)
        // Perdoando ontem: hoje + ontem + 3 dias anteriores = 5.
        assertEquals(5, offer.restoredStreak)
    }

    @Test
    fun `no revive is offered when the user has none left`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 10),
            usage(daysAgo = 1, minutes = 95),
            usage(daysAgo = 2, minutes = 5)
        )

        val result = calculateStreakResult(
            logs, listOf(monitored()), today = TODAY, state = NO_REVIVES
        )

        assertEquals(1, result.currentStreak)
        assertNull(result.reviveOffer)
    }

    @Test
    fun `no revive is offered while the streak is still intact`() {
        val logs = (0..3).map { usage(daysAgo = it, minutes = 5) }

        val result = calculateStreakResult(logs, listOf(monitored()), today = TODAY)

        assertEquals(4, result.currentStreak)
        assertNull(result.reviveOffer)
    }

    @Test
    fun `going over today alone does not offer a revive because today is not over`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 95),   // hoje estourou
            usage(daysAgo = 1, minutes = 5)
        )

        val result = calculateStreakResult(logs, listOf(monitored()), today = TODAY)

        assertEquals(0, result.currentStreak)
        assertNull(result.reviveOffer)
    }

    // -----------------------------------------------------------------------
    // Revive: usar
    // -----------------------------------------------------------------------

    @Test
    fun `using the revive restores the streak and spends the revive`() {
        val logs = listOf(
            usage(daysAgo = 0, minutes = 10),
            usage(daysAgo = 1, minutes = 95),
            usage(daysAgo = 2, minutes = 5),
            usage(daysAgo = 3, minutes = 5)
        )
        val settings = listOf(monitored())

        val before = StreakState()
        val offer = calculateStreakResult(logs, settings, today = TODAY, state = before).reviveOffer!!

        val after = useRevive(before, offer.brokenDate)

        assertEquals(0, after.revivesAvailable)
        assertEquals(4, calculateStreak(logs, settings, today = TODAY, state = after))
        // E a oferta some, porque não sobrou revive.
        assertNull(calculateStreakResult(logs, settings, today = TODAY, state = after).reviveOffer)
    }

    @Test
    fun `using a revive with none available changes nothing`() {
        val after = useRevive(NO_REVIVES, TODAY.minusDays(1))

        assertEquals(NO_REVIVES, after)
    }

    @Test
    fun `a revived day also shows as held in the week strip`() {
        val logs = listOf(usage(daysAgo = 1, minutes = 95))
        val settings = listOf(monitored())
        val broken = TODAY.minusDays(1)

        assertEquals(false, dayHeldLimit(logs, settings, broken))

        val after = useRevive(StreakState(), broken)
        assertEquals(true, dayHeldLimit(logs, settings, broken, after))
    }

    // -----------------------------------------------------------------------
    // Revive: ganhar de volta a cada 7 dias
    // -----------------------------------------------------------------------

    @Test
    fun `reaching seven days grants a new revive after one was spent`() {
        val spent = StreakState(revivesAvailable = 0, milestonesRewarded = 0)

        val afterSixDays = grantMilestoneRevives(spent, currentStreak = 6)
        assertEquals(0, afterSixDays.revivesAvailable)

        val afterSevenDays = grantMilestoneRevives(spent, currentStreak = 7)
        assertEquals(1, afterSevenDays.revivesAvailable)
        assertEquals(1, afterSevenDays.milestonesRewarded)
    }

    @Test
    fun `the same milestone never grants a second revive`() {
        val spent = StreakState(revivesAvailable = 0)

        val once = grantMilestoneRevives(spent, currentStreak = 7)
        // A tela recalcula várias vezes com a mesma streak de 7 dias.
        val twice = grantMilestoneRevives(once, currentStreak = 7)
        val thrice = grantMilestoneRevives(twice, currentStreak = 9)

        assertEquals(1, thrice.revivesAvailable)
        assertEquals(1, thrice.milestonesRewarded)
    }

    @Test
    fun `revives never stack beyond the cap of one`() {
        // Chegou aos 14 dias sem nunca gastar: continua com 1, não 2.
        val full = StreakState(revivesAvailable = 1, milestonesRewarded = 0)

        val after = grantMilestoneRevives(full, currentStreak = 14)

        assertEquals(MAX_REVIVES, after.revivesAvailable)
        assertEquals(2, after.milestonesRewarded)
    }

    @Test
    fun `revived dates survive the round trip through the database string`() {
        val first = useRevive(StreakState(), TODAY.minusDays(3))
        // Simula ter ganhado outro revive num marco de 7 dias.
        val earnedAnother = first.copy(revivesAvailable = 1)
        val second = useRevive(earnedAnother, TODAY.minusDays(1))

        assertEquals(
            setOf(TODAY.minusDays(3), TODAY.minusDays(1)),
            second.revivedDateSet()
        )
        assertEquals(0, second.revivesAvailable)
    }

    @Test
    fun `an empty revived dates string parses to an empty set`() {
        assertEquals(emptySet<LocalDate>(), StreakState().revivedDateSet())
    }
}
