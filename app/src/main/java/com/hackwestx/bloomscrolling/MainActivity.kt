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
import com.hackwestx.bloomscrolling.navigation.BloomNavGraph
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
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // Toda a navegação do app vive no NavGraph.
                        // weight(1f) = o NavGraph ocupa todo o espaço que
                        // sobrar depois do botão de debug lá embaixo.
                        BloomNavGraph(modifier = Modifier.weight(1f))
                        DebugSurveyButton()
                    }
                }
            }
        }
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
