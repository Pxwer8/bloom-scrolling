package com.hackwestx.bloomscrolling.gamification

import com.hackwestx.bloomscrolling.data.SurveyResponse

// ---------------------------------------------------------------------------
// Três conquistas fixas, sem tabela no banco: elas são DERIVADAS dos dados que
// já existem (usage_log + survey_response), então não há nada a salvar — é só
// recalcular quando a tela abrir. Menos estado = menos bug.
// ---------------------------------------------------------------------------

/**
 * O `reason` que uma resposta "pulada" teria no banco.
 *
 * ATENÇÃO: hoje o botão "Proceed anyway" da SurveyActivity **não grava nada** —
 * ele só fecha a tela. Ou seja, toda linha em survey_response já é, por
 * definição, uma resposta de verdade. Esta constante existe para o dia em que
 * a equipe decidir registrar também as recusas: basta gravar uma linha com
 * este reason e a contagem abaixo continua correta sem mudar nada.
 */
const val SKIPPED_REASON = "proceed_anyway"

/** Quantas vezes o usuário respondeu de fato, em vez de pular a pergunta. */
fun countAnsweredSurveys(responses: List<SurveyResponse>): Int =
    responses.count { it.reason != SKIPPED_REASON }

/**
 * As conquistas do app. Um `enum` (e não uma lista de Strings) porque assim o
 * compilador garante que o `when` mais abaixo trate todas — se alguém
 * adicionar uma quarta conquista, o código não compila até ela ser tratada.
 */
enum class Achievement(
    val title: String,
    val description: String
) {
    THREE_DAY_STREAK(
        title = "3-day streak",
        description = "Three days in a row within your limits."
    ),
    CLOSED_APP_AFTER_SURVEY(
        title = "Closed app after survey",
        description = "You answered the check-in instead of scrolling past it."
    ),
    UNDER_LIMIT_ALL_WEEK(
        title = "Under limit all week",
        description = "Seven days in a row within your limits."
    )
}

/**
 * Tudo que é preciso saber para avaliar as conquistas, num pacote só.
 * Quem chama monta isto uma vez (com [calculateStreak] e [countAnsweredSurveys])
 * e as três checagens saem de graça.
 */
data class AchievementProgress(
    val currentStreak: Int,
    val answeredSurveys: Int
)

/** Esta conquista já foi alcançada? */
fun Achievement.isUnlocked(progress: AchievementProgress): Boolean = when (this) {
    Achievement.THREE_DAY_STREAK -> progress.currentStreak >= 3
    Achievement.CLOSED_APP_AFTER_SURVEY -> progress.answeredSurveys >= 1
    Achievement.UNDER_LIMIT_ALL_WEEK -> progress.currentStreak >= DAYS_IN_WEEK
}

/** As três conquistas com seu status — pronto para virar uma lista na tela. */
fun evaluateAchievements(progress: AchievementProgress): Map<Achievement, Boolean> =
    Achievement.entries.associateWith { it.isUnlocked(progress) }

/** Só as já alcançadas. */
fun unlockedAchievements(progress: AchievementProgress): List<Achievement> =
    Achievement.entries.filter { it.isUnlocked(progress) }
