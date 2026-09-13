package com.hackwestx.bloomscrolling.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackwestx.bloomscrolling.data.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonitoredAppLimit(
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int
)

private val SURVEY_FREQUENCY_MINUTES = intPreferencesKey("survey_frequency_minutes")
private val FILTER_INTENSITY = floatPreferencesKey("filter_intensity")

// Neither the schema nor the plan doc specifies these — same kind of
// placeholder as App Selection's default daily limit, not a product decision.
private const val DEFAULT_SURVEY_FREQUENCY_MINUTES = 5
private const val DEFAULT_FILTER_INTENSITY = 0.5f

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    // Per-app limits, sourced from Room — only apps currently monitored
    // (isBlocked) show up here, same set App Selection toggles on.
    val monitoredApps: StateFlow<List<MonitoredAppLimit>> = settingsDao.observeAll()
        .map { settings ->
            settings.filter { it.isBlocked }.map { s ->
                MonitoredAppLimit(
                    packageName = s.packageName,
                    appName = resolveAppName(context, s.packageName),
                    dailyLimitMinutes = s.dailyLimitMinutes
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Global preferences, sourced from DataStore — not per-app.
    // NOTE: survey frequency is stored here but not yet enforced anywhere.
    // MonitorAccessibilityService currently fires the survey on every single
    // blocklisted-app open with no cooldown. Respecting this setting needs a
    // per-app "last shown" timestamp on UserSettings, which is the RoomSchema
    // change flagged as a pending team decision, not implemented here.
    val surveyFrequencyMinutes: StateFlow<Int> = dataStore.data
        .map { it[SURVEY_FREQUENCY_MINUTES] ?: DEFAULT_SURVEY_FREQUENCY_MINUTES }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_SURVEY_FREQUENCY_MINUTES)

    val filterIntensity: StateFlow<Float> = dataStore.data
        .map { it[FILTER_INTENSITY] ?: DEFAULT_FILTER_INTENSITY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_FILTER_INTENSITY)

    fun setDailyLimit(packageName: String, minutes: Int) {
        viewModelScope.launch {
            val existing = settingsDao.get(packageName) ?: return@launch
            settingsDao.upsert(existing.copy(dailyLimitMinutes = minutes))
        }
    }

    fun setSurveyFrequency(minutes: Int) {
        viewModelScope.launch {
            dataStore.edit { it[SURVEY_FREQUENCY_MINUTES] = minutes }
        }
    }

    fun setFilterIntensity(intensity: Float) {
        viewModelScope.launch {
            dataStore.edit { it[FILTER_INTENSITY] = intensity }
        }
    }
}

private fun resolveAppName(context: Context, packageName: String): String =
    try {
        // minSdk is 26; the ApplicationInfoFlags overload needs API 33+, so
        // this uses the older int-flag overload (deprecated on 33+, still works).
        @Suppress("DEPRECATION")
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
