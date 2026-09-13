package com.hackwestx.bloomscrolling

import android.app.Application
import com.hackwestx.bloomscrolling.data.DemoSeed
import com.hackwestx.bloomscrolling.data.SettingsDao
import com.hackwestx.bloomscrolling.data.SurveyDao
import com.hackwestx.bloomscrolling.data.UsageDao
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BloomScrollingApplication : Application() {

    // O Hilt preenche estes campos durante super.onCreate(), então só dá para
    // usá-los depois daquela chamada.
    @Inject lateinit var usageDao: UsageDao
    @Inject lateinit var settingsDao: SettingsDao
    @Inject lateinit var surveyDao: SurveyDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Dados de demo: só em build de debug, e o próprio DemoSeed desiste se
        // o banco já tiver apps configurados. Roda fora da thread principal
        // porque escrever no Room bloqueia.
        if (BuildConfig.DEBUG) {
            scope.launch {
                DemoSeed.seedIfEmpty(
                    context = this@BloomScrollingApplication,
                    usageDao = usageDao,
                    settingsDao = settingsDao,
                    surveyDao = surveyDao
                )
            }
        }
    }
}
