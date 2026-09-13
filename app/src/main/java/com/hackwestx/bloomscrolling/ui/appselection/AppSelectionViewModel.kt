package com.hackwestx.bloomscrolling.ui.appselection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackwestx.bloomscrolling.data.SettingsDao
import com.hackwestx.bloomscrolling.data.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap,
    val isMonitored: Boolean
)

// No default daily limit is specified anywhere in the schema/planning docs —
// this is a placeholder until product decides what a newly-monitored app's
// limit should default to.
private const val DEFAULT_DAILY_LIMIT_MINUTES = 30

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())

    val apps: StateFlow<List<AppInfo>> = combine(
        launchableApps,
        settingsDao.observeAll()
    ) { launchable, settings ->
        val settingsByPackage = settings.associateBy { it.packageName }
        launchable.map { app ->
            AppInfo(
                packageName = app.packageName,
                appName = app.appName,
                icon = app.icon,
                isMonitored = settingsByPackage[app.packageName]?.isBlocked ?: false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            launchableApps.value = withContext(Dispatchers.Default) {
                queryLaunchableApps(context)
            }
        }
    }

    fun setMonitored(packageName: String, monitored: Boolean) {
        viewModelScope.launch {
            val existing = settingsDao.get(packageName)
            settingsDao.upsert(
                UserSettings(
                    packageName = packageName,
                    dailyLimitMinutes = existing?.dailyLimitMinutes ?: DEFAULT_DAILY_LIMIT_MINUTES,
                    isBlocked = monitored
                )
            )
        }
    }
}

private data class LaunchableApp(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap
)

private fun queryLaunchableApps(context: Context): List<LaunchableApp> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    return packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { resolveInfo ->
            LaunchableApp(
                packageName = resolveInfo.activityInfo.packageName,
                appName = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager).toBitmap().asImageBitmap()
            )
        }
        .sortedBy { it.appName.lowercase() }
}
