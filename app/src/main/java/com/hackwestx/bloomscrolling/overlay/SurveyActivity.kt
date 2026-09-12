package com.hackwestx.bloomscrolling.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.hackwestx.bloomscrolling.data.SurveyDao
import com.hackwestx.bloomscrolling.data.SurveyResponse
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
            // Sem BloomScrollingTheme aqui de propósito: aquele tema puxa as
            // cores dinâmicas do sistema (roxo, azul, o que o usuário tiver).
            // Esta tela define cada cor na mão para ficar 100% monocromática.
            SurveyContent(
                onAnswer = { reason -> saveAndClose(reason, triggeringPackage) },
                onProceedAnyway = { finish() }
            )
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

// ---------------------------------------------------------------------------
// Visual: preto, branco e cinza. Nada de cor vibrante, nada de sombra, nada
// de animação — "fricção visual reduzida" significa que a tela não compete
// por atenção; ela só pausa o usuário por um segundo.
// ---------------------------------------------------------------------------

private data class SurveyPalette(
    val scrim: Color,     // escurece o app que está atrás
    val surface: Color,   // fundo do painel
    val text: Color,      // pergunta e rótulos dos botões
    val muted: Color,     // texto secundário ("Proceed anyway")
    val border: Color     // contorno fino dos botões
)

private val LightPalette = SurveyPalette(
    scrim = Color(0xFF000000).copy(alpha = 0.55f),
    surface = Color(0xFFFAFAFA),
    text = Color(0xFF111111),
    muted = Color(0xFF757575),
    border = Color(0xFFDCDCDC)
)

private val DarkPalette = SurveyPalette(
    scrim = Color(0xFF000000).copy(alpha = 0.72f),
    surface = Color(0xFF141414),
    text = Color(0xFFF2F2F2),
    muted = Color(0xFF8A8A8A),
    border = Color(0xFF2E2E2E)
)

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
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette

    // A janela é transparente, então este Box é o "vidro fosco" que
    // escurece o app de trás sem escondê-lo por completo.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(4.dp), // canto quase reto, sem "bolha"
            tonalElevation = 0.dp,            // zero sombra: nada flutua
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)        // não estica demais em tablet
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Why are you opening this right now?",
                    color = palette.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal, // peso normal: pergunta, não alarme
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    REASON_OPTIONS.forEach { (label, storedValue) ->
                        ReasonButton(
                            label = label,
                            palette = palette,
                            onClick = { onAnswer(storedValue) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Saída sem registrar resposta: deliberadamente discreta —
                // cinza, sem contorno, menor que as opções acima.
                TextButton(
                    onClick = onProceedAnyway,
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.muted),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Proceed anyway", fontSize = 13.sp)
                }
            }
        }
    }
}

/** Botão de resposta: contorno fino, fundo transparente, texto escuro. */
@Composable
private fun ReasonButton(
    label: String,
    palette: SurveyPalette,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, palette.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = palette.text
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.fillMaxWidth() // alinha todos os rótulos à esquerda
        )
    }
}
