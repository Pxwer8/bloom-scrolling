package com.hackwestx.bloomscrolling.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.hackwestx.bloomscrolling.data.SettingsDao
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

        // Debounce: TYPE_WINDOW_STATE_CHANGED fires repeatedly for the same
        // foreground app (dialogs, keyboard, etc.) — only act on a real switch.
        if (packageName == lastPackage) return
        lastPackage = packageName

        scope.launch {
            val settings = settingsDao.get(packageName)
            if (settings?.isBlocked == true) {
                onBlocklistedAppOpened(packageName)
            }
        }
    }

    private fun onBlocklistedAppOpened(packageName: String) {
        // This is the single trigger point everything else hangs off:
        //   1. Person A: check survey-due state, launch SurveyActivity if due
        //   2. Person B: start/refresh UsageTimerService for this packageName
        // Wire these in during Phase 4 integration, not before — build and
        // test each piece against a fake trigger (a button in a debug screen)
        // first, so you're not debugging two new systems at once.
    }

    override fun onInterrupt() {}
}
