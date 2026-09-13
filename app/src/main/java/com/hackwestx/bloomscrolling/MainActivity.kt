package com.hackwestx.bloomscrolling

import android.Manifest
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.hackwestx.bloomscrolling.navigation.BloomNavGraph
import com.hackwestx.bloomscrolling.navigation.Routes
import com.hackwestx.bloomscrolling.overlay.SurveyActivity
import com.hackwestx.bloomscrolling.service.MyForegroundService
import com.hackwestx.bloomscrolling.ui.theme.BloomScrollingTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            MyForegroundService.start(this)
        } else {
            Toast.makeText(
                this,
                "Sem a permissão, a notificação não aparece",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloomScrollingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // Toda a navegação do app vive no NavGraph.
                        // weight(1f) = o NavGraph ocupa todo o espaço que
                        // sobrar depois dos botões de debug lá embaixo.
                        BloomNavGraph(
                            modifier = Modifier.weight(1f),
                            navController = navController
                        )
                        // BuildConfig.DEBUG é `true` só no build de debug. Como
                        // é uma constante conhecida em tempo de compilação, o
                        // compilador remove este bloco inteiro do APK de
                        // release — o código fica aqui, mas não é publicado.
                        if (BuildConfig.DEBUG) {
                            DebugSettingsButton(onClick = { navController.navigate(Routes.SETTINGS) })
                            DebugSurveyButton()
                            DebugStartServiceButton(onClick = { startServiceWithPermissionCheck() })
                        }
                    }
                }
            }
        }
    }

    private fun startServiceWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                MyForegroundService.start(this)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            MyForegroundService.start(this)
        }
    }
}

/**
 * TEMPORÁRIO — remover assim que existir uma navegação real (bottom nav /
 * drawer) para chegar em Settings. Por enquanto é a única forma de abrir
 * a tela fora do fluxo linear onboarding -> app_selection.
 */
@Composable
private fun DebugSettingsButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("DEBUG: Open Settings")
    }
}

/**
 * TEMPORÁRIO — remover na integração (Fase 4).
 * Simula a detecção do Sam: abre a SurveyActivity na mão, sem esperar o
 * AccessibilityService detectar um app bloqueado.
 */
@Composable
private fun DebugSurveyButton() {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { context.startActivity(SurveyActivity.newIntent(context)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("DEBUG: Open Survey")
    }
}

/**
 * TEMPORÁRIO — remover quando o serviço em segundo plano passar a ser
 * iniciado automaticamente pelo fluxo normal do app (Fase 2+).
 */
@Composable
private fun DebugStartServiceButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("DEBUG: Start Service")
    }
}

@Composable
fun UsageStatsSpike(modifier: Modifier = Modifier, onStartService: () -> Unit = {}) {
    val context = LocalContext.current
    var resultText by remember { mutableStateOf("Toque no botão para verificar.") }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Hello Android!")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            resultText = checkLastForegroundApp(context)
        }) {
            Text("Check Usage Stats")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = resultText)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onStartService() }) {
            Text("Start Service")
        }
    }
}

private fun checkLastForegroundApp(context: Context): String {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()
    val tenMinutesAgo = now - 10 * 60 * 1000
    val ownPackage = context.packageName

    val events = usageStatsManager.queryEvents(tenMinutesAgo, now)
    val event = UsageEvents.Event()

    var lastPackage: String? = null
    var lastEventTime: Long = 0

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val isOwnApp = event.packageName == ownPackage
        val isLauncher = event.packageName.contains("launcher", ignoreCase = true)
        if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && !isOwnApp && !isLauncher) {
            lastPackage = event.packageName
            lastEventTime = event.timeStamp
        }
    }

    return if (lastPackage != null) {
        val secondsAgo = (now - lastEventTime) / 1000
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(lastEventTime)
        "Último app real: $lastPackage\nRegistrado às: $timeFormat\nHá $secondsAgo segundos"
    } else {
        "Nenhum outro app real encontrado nos últimos 10 minutos. Abra um app e volte."
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BloomScrollingTheme {
        UsageStatsSpike()
    }
}
