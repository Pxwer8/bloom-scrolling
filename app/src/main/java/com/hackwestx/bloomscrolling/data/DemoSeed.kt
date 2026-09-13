package com.hackwestx.bloomscrolling.data

import android.content.Context
import android.util.Log
import com.hackwestx.bloomscrolling.util.daysAgo
import kotlinx.coroutines.flow.first

// ---------------------------------------------------------------------------
// DADOS DE DEMO — só roda em build de debug e só quando o banco está vazio.
//
// Sem isto, um aparelho recém-instalado mostra 0% na Home, 0 dias de streak e
// todas as conquistas travadas, porque usage_log só se enche com uso real ao
// longo de dias. Os números abaixo são fixos de propósito (nada de random):
// uma demo tem que dar o mesmo resultado toda vez que for rodada.
// ---------------------------------------------------------------------------

object DemoSeed {

    private const val TAG = "BloomScrolling"
    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /** Dia, contado para trás a partir de hoje, em que o usuário estourou. */
    private const val BROKEN_DAY_OFFSET = 3

    private data class DemoApp(
        val packageName: String,
        val dailyLimitMinutes: Int,
        /** Minutos por dia; o índice é "quantos dias atrás", 0 = hoje. */
        val minutesByDayOffset: List<Int>
    )

    /**
     * Cada app carrega a própria série de minutos, em vez de a série ficar
     * numa tabela separada indexada por posição. Assim, se um dos apps não
     * estiver instalado no aparelho e cair fora, os números dos outros não
     * se embaralham.
     *
     * Orçamento total = 105 min/dia. No dia [BROKEN_DAY_OFFSET] a soma dá 180,
     * e cada app sozinho também estoura o próprio limite naquele dia — então o
     * card de revive aparece na demo mesmo que só um deles esteja instalado.
     */
    private val CANDIDATE_APPS = listOf(
        DemoApp(
            packageName = "com.instagram.android",
            dailyLimitMinutes = 30,
            //                   hoje  1   2   3*   4   5   6   7   8   9
            minutesByDayOffset = listOf(12, 20, 5, 65, 18, 25, 10, 22, 8, 30)
        ),
        DemoApp(
            packageName = "com.zhiliaoapp.musically", // TikTok
            dailyLimitMinutes = 30,
            minutesByDayOffset = listOf(8, 15, 10, 55, 12, 20, 5, 18, 12, 25)
        ),
        DemoApp(
            packageName = "com.google.android.youtube",
            dailyLimitMinutes = 45,
            minutesByDayOffset = listOf(10, 25, 20, 60, 30, 15, 35, 20, 25, 40)
        )
    )

    private const val DAYS = 10

    /** Pickups do dia, divididos entre os apps. Índice = dias atrás. */
    private val DAILY_PICKUPS = listOf(4, 7, 3, 19, 6, 8, 5, 7, 4, 11)

    /**
     * Algumas respostas de survey, para a conquista "Closed app after survey"
     * aparecer desbloqueada. EXTRA — não é necessário para o streak nem para a
     * Home; se atrapalhar, basta apagar esta lista e a chamada lá embaixo.
     */
    private val SURVEY_ANSWERS = listOf(
        0 to "bored",
        1 to "habit",
        2 to "specific_task",
        3 to "bored",
        3 to "habit",
        5 to "messaging"
    )

    /**
     * Preenche o banco se ainda não houver nenhum app configurado.
     *
     * A checagem por user_settings vazio é o que impede o seed de passar por
     * cima de dados reais: se a pessoa já escolheu apps na tela de seleção,
     * nada aqui roda.
     */
    suspend fun seedIfEmpty(
        context: Context,
        usageDao: UsageDao,
        settingsDao: SettingsDao,
        surveyDao: SurveyDao
    ) {
        if (settingsDao.observeAll().first().isNotEmpty()) {
            Log.d(TAG, "DemoSeed: banco já tem apps configurados, nada a fazer")
            return
        }

        val apps = pickApps(context)

        apps.forEach { app ->
            settingsDao.upsert(
                UserSettings(
                    packageName = app.packageName,
                    dailyLimitMinutes = app.dailyLimitMinutes,
                    isBlocked = true
                )
            )
        }

        for (offset in 0 until DAYS) {
            apps.forEach { app ->
                usageDao.upsert(
                    UsageLog(
                        date = daysAgo(offset),
                        packageName = app.packageName,
                        totalMinutes = app.minutesByDayOffset[offset],
                        // Divide os pickups do dia entre os apps, sempre pelo
                        // menos 1 — um app com minutos registrados foi aberto.
                        pickupCount = (DAILY_PICKUPS[offset] / apps.size).coerceAtLeast(1)
                    )
                )
            }
        }

        val now = System.currentTimeMillis()
        SURVEY_ANSWERS.forEach { (offset, reason) ->
            surveyDao.insert(
                SurveyResponse(
                    timestamp = now - offset * MILLIS_PER_DAY,
                    packageName = apps.first().packageName,
                    reason = reason
                )
            )
        }

        Log.d(
            TAG,
            "DemoSeed: ${apps.size} apps e $DAYS dias inseridos " +
                "(dia estourado: ${daysAgo(BROKEN_DAY_OFFSET)})"
        )
    }

    /**
     * Prefere os apps que estão realmente instalados no aparelho, para as
     * telas mostrarem "Instagram" em vez de "com.instagram.android" (o
     * PackageManager não resolve o nome de um app que não existe).
     * Se nenhum dos candidatos estiver instalado, usa a lista como está.
     */
    private fun pickApps(context: Context): List<DemoApp> {
        val installed = CANDIDATE_APPS.filter { app ->
            runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(app.packageName, 0)
            }.isSuccess
        }
        return installed.ifEmpty { CANDIDATE_APPS }
    }
}
