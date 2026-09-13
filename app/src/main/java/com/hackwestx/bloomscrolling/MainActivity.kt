package com.hackwestx.bloomscrolling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.hackwestx.bloomscrolling.navigation.BloomNavGraph
import com.hackwestx.bloomscrolling.navigation.Routes
import com.hackwestx.bloomscrolling.overlay.SurveyActivity
import com.hackwestx.bloomscrolling.ui.theme.BloomScrollingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
                        DebugSettingsButton(onClick = { navController.navigate(Routes.SETTINGS) })
                        DebugSurveyButton()
                    }
                }
            }
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
