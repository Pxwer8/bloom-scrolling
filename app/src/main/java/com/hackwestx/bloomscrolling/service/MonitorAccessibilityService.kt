package com.hackwestx.bloomscrolling.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.hackwestx.bloomscrolling.data.SettingsDao
import com.hackwestx.bloomscrolling.data.UserSettings
import com.hackwestx.bloomscrolling.overlay.SurveyActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

// -----------------------------------------------------------------------
// PHASE 2 CRITICAL PATH. Get this file working end-to-end (registered,
// receiving events, correctly reading packageName) before anyone starts
// Phase 3. Log every event to Logcat while you're testing it — don't
// wire up the survey/overlay until you've watched real events come through.
// -----------------------------------------------------------------------

@AndroidEntryPoint
class MonitorAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var settingsDao: SettingsDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastPackage: String? = null

    private companion object {
        const val TAG = "BloomScrolling"

        // 30 segundos: valor reduzido INTENCIONALMENTE para a demo do
        // hackathon, ver Phase 6 do plano original. Não é placeholder
        // esquecido — com 5 minutos ninguém consegue mostrar o cooldown
        // funcionando numa apresentação de poucos minutos.
        //
        // Valor pensado para uso real: 5 * 60 * 1000L (5 minutos), tempo
        // suficiente para não repetir a pergunta numa ida e volta rápida
        // (abrir o Instagram, olhar uma notificação, voltar) sem deixar a
        // pessoa passar sessões longas sem nenhum check-in.
        const val SURVEY_COOLDOWN_MILLIS = 30 * 1000L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Ignore our own windows (SurveyActivity) — otherwise closing it back
        // into the blocked app reads as a "new" switch and re-fires the survey.
        if (packageName == applicationContext.packageName) return

        // Debounce: TYPE_WINDOW_STATE_CHANGED fires repeatedly for the same
        // foreground app (dialogs, keyboard, etc.) — only act on a real switch.
        if (packageName == lastPackage) return
        lastPackage = packageName

        android.util.Log.d("BloomScrolling", "App switched to: $packageName")

        scope.launch {
            val settings = settingsDao.get(packageName)
            if (settings?.isBlocked == true) {
                onBlocklistedAppOpened(settings)
            } else {
                // Trocou para um app que não é monitorado: fecha a contagem que
                // estiver aberta e tira o filtro de cor. A decisão de blocklist
                // acima não mudou — isto é só o outro lado do gatilho do timer
                // e do filtro.
                UsageTimerService.stop(this@MonitorAccessibilityService)
                ColorFilterService.stop(this@MonitorAccessibilityService)
            }
        }
    }

    /**
     * Chamado só quando o app já foi confirmado como bloqueado (o `isBlocked`
     * é decidido por quem chama — esta função não mexe nessa regra).
     */
    private suspend fun onBlocklistedAppOpened(settings: UserSettings) {
        val packageName = settings.packageName

        // Timer primeiro, survey depois: a SurveyActivity é do nosso próprio
        // pacote, e o filtro lá em cima ignora eventos dela — então abrir a
        // pergunta não interrompe a contagem que acabou de começar.
        //
        // A contagem de uso NÃO é afetada pelo cooldown: ela começa toda vez,
        // com ou sem pergunta. O cooldown silencia só a survey.
        UsageTimerService.start(this, packageName)
        ColorFilterService.start(this)

        val now = System.currentTimeMillis()
        val elapsedSinceLastSurvey = now - settings.lastSurveyTimestamp

        if (elapsedSinceLastSurvey < SURVEY_COOLDOWN_MILLIS) {
            android.util.Log.d(
                TAG,
                "Survey em cooldown para $packageName " +
                    "(${elapsedSinceLastSurvey / 1000}s de ${SURVEY_COOLDOWN_MILLIS / 1000}s)"
            )
            return
        }

        // Grava o carimbo ANTES de abrir a tela. Se gravássemos depois, duas
        // trocas quase simultâneas poderiam passar as duas pela checagem acima
        // e abrir a survey em duplicado.
        settingsDao.upsert(settings.copy(lastSurveyTimestamp = now))

        startActivity(SurveyActivity.newIntent(this, packageName))
    }

    override fun onInterrupt() {}
}
