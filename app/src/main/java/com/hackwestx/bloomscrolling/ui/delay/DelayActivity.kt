package com.hackwestx.bloomscrolling.ui.delay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Tela que aparece por cima do app monitorado forçando uma pausa antes de
 * continuar. Mesmo padrão da SurveyActivity: Activity separada (não uma rota
 * do NavGraph) porque precisa abrir de fora do app, pelo AccessibilityService.
 */
class DelayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val triggeringPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: UNKNOWN_PACKAGE

        setContent {
            // Sem BloomScrollingTheme aqui de propósito, mesmo motivo da
            // SurveyActivity: cores na mão, 100% monocromática.
            DelayContent(
                packageName = triggeringPackage,
                onContinueAnyway = { finish() },
                onCloseApp = { goHome() }
            )
        }
    }

    /** Manda o usuário pra tela inicial do sistema em vez de voltar pro app bloqueado. */
    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val UNKNOWN_PACKAGE = "unknown"

        /** Jeito único de abrir esta tela — mesmo formato do SurveyActivity.newIntent. */
        fun newIntent(context: Context, packageName: String? = null): Intent =
            Intent(context, DelayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                packageName?.let { putExtra(EXTRA_PACKAGE_NAME, it) }
            }
    }
}

// ---------------------------------------------------------------------------
// Mesma linguagem visual da SurveyActivity: preto, branco, cinza. A única
// "animação" é a respiração do círculo — ela é o conteúdo, não decoração.
// ---------------------------------------------------------------------------

private data class DelayPalette(
    val scrim: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val breathingCircle: Color
)

private val LightPalette = DelayPalette(
    scrim = Color(0xFF000000).copy(alpha = 0.55f),
    surface = Color(0xFFFAFAFA),
    text = Color(0xFF111111),
    muted = Color(0xFF757575),
    border = Color(0xFFDCDCDC),
    breathingCircle = Color(0xFF111111)
)

private val DarkPalette = DelayPalette(
    scrim = Color(0xFF000000).copy(alpha = 0.72f),
    surface = Color(0xFF141414),
    text = Color(0xFFF2F2F2),
    muted = Color(0xFF8A8A8A),
    border = Color(0xFF2E2E2E),
    breathingCircle = Color(0xFFF2F2F2)
)

private const val COUNTDOWN_START = 15

@Composable
private fun DelayContent(
    packageName: String,
    onContinueAnyway: () -> Unit,
    onCloseApp: () -> Unit
) {
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    var secondsLeft by remember { mutableIntStateOf(COUNTDOWN_START) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(4.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Take a moment before you continue",
                    color = palette.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 28.sp
                )

                Box(modifier = Modifier.height(24.dp))

                BreathingCircle(color = palette.breathingCircle)

                Box(modifier = Modifier.height(16.dp))

                Text(
                    text = if (secondsLeft > 0) "$secondsLeft" else "Ready",
                    color = palette.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal
                )

                Box(modifier = Modifier.height(8.dp))

                Text(
                    text = packageName,
                    color = palette.muted,
                    fontSize = 12.sp
                )

                Box(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onContinueAnyway,
                    enabled = secondsLeft == 0,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = palette.text,
                        disabledContentColor = palette.muted
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Continue anyway", fontSize = 15.sp)
                }

                Box(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onCloseApp,
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.muted),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Close app", fontSize = 13.sp)
                }
            }
        }
    }
}

/** Círculo que cresce e encolhe continuamente, ritmo de respiração (~4s por ciclo). */
@Composable
private fun BreathingCircle(color: Color) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .scale(scale)
            .background(color = color.copy(alpha = 0.15f), shape = CircleShape)
    )
}
