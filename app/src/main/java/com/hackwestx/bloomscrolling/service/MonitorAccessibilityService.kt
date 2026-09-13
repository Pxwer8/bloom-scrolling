package com.hackwestx.bloomscrolling.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.hackwestx.bloomscrolling.data.SettingsDao
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
                onBlocklistedAppOpened(packageName)
            } else {
                // Trocou para um app que não é monitorado: fecha a contagem que
                // estiver aberta. A decisão de blocklist acima não mudou — isto
                // é só o outro lado do gatilho do timer.
                UsageTimerService.stop(this@MonitorAccessibilityService)
            }
        }
    }

    private fun onBlocklistedAppOpened(packageName: String) {
        // "Due" is currently just "isBlocked" (already checked by the
        // caller) — every open of a blocked app triggers the survey.
        // ponytail: no cooldown yet, will ask on every single switch even
        // seconds apart. Add a lastSurveyTimestamp to UserSettings if that
        // turns out to be annoying in testing.
        // Timer primeiro, survey depois: a SurveyActivity é do nosso próprio
        // pacote, e o filtro lá em cima ignora eventos dela — então abrir a
        // pergunta não interrompe a contagem que acabou de começar.
        UsageTimerService.start(this, packageName)

        startActivity(SurveyActivity.newIntent(this, packageName))
    }

    override fun onInterrupt() {}
}
