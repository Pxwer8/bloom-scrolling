package com.hackwestx.bloomscrolling.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.hackwestx.bloomscrolling.data.SurveyDao
import com.hackwestx.bloomscrolling.data.SurveyResponse
import com.hackwestx.bloomscrolling.ui.theme.BloomScrollingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tela que aparece por cima do app monitorado perguntando por que o usuário
 * está abrindo aquele app. É uma Activity separada (e não uma rota do NavGraph)
 * porque ela precisa ser aberta de fora do app — pelo AccessibilityService,
 * enquanto o Instagram/TikTok está na frente.
 */
@AndroidEntryPoint
class SurveyActivity : ComponentActivity() {

    // O Hilt entrega o SurveyDao pronto (AppModule.provideSurveyDao).
    @Inject lateinit var surveyDao: SurveyDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Qual app disparou a pergunta. Quem abre a Activity manda esse extra;
        // no botão de debug não tem nada, então cai no "unknown".
        val triggeringPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: UNKNOWN_PACKAGE

        setContent {
            BloomScrollingTheme {
                SurveyContent(
                    onAnswer = { reason -> saveAndClose(reason, triggeringPackage) },
                    onProceedAnyway = { finish() }
                )
            }
        }
    }

    /** Grava a resposta no banco e só então fecha a tela. */
    private fun saveAndClose(reason: String, packageName: String) {
        lifecycleScope.launch {
            surveyDao.insert(
                SurveyResponse(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    reason = reason
                )
            )
            finish()
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val UNKNOWN_PACKAGE = "unknown"

        /**
         * Jeito único de abrir esta tela. Quando a detecção do Sam ficar pronta,
         * o AccessibilityService chama exatamente isto passando o pacote real.
         */
        fun newIntent(context: Context, packageName: String? = null): Intent =
            Intent(context, SurveyActivity::class.java).apply {
                // NEW_TASK: obrigatório quando quem abre não é uma Activity
                // (um Service, por exemplo).
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                packageName?.let { putExtra(EXTRA_PACKAGE_NAME, it) }
            }
    }
}

// Texto do botão (inglês, o que o usuário vê) -> valor salvo no banco,
// no mesmo formato que o SurveyResponse documenta.
private val REASON_OPTIONS = listOf(
    "Bored" to "bored",
    "Habit" to "habit",
    "Specific task" to "specific_task",
    "Messaging someone" to "messaging"
)

@Composable
private fun SurveyContent(
    onAnswer: (String) -> Unit,
    onProceedAnyway: () -> Unit
) {
    // A janela é transparente, então esse Box escuro é o "vidro fosco"
    // que escurece o app que está atrás.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Why are you opening this right now?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                REASON_OPTIONS.forEach { (label, storedValue) ->
                    Button(
                        onClick = { onAnswer(storedValue) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }
                TextButton(
                    onClick = onProceedAnyway,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Proceed anyway")
                }
            }
        }
    }
}
