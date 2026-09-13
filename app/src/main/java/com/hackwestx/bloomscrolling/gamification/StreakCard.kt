package com.hackwestx.bloomscrolling.gamification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Standalone: recebe um Int e desenha. Não conhece banco, ViewModel nem
// navegação — dá pra encaixar no Dashboard depois com uma linha só:
//     StreakCard(days = uiState.streakDays)
//
// Mesma paleta preto/branco/cinza da SurveyActivity. Está duplicada aqui de
// propósito para o arquivo continuar independente; quando aparecer um terceiro
// uso, vale mover para ui/theme/ e compartilhar.
// ---------------------------------------------------------------------------

private data class CardPalette(
    val surface: Color,
    val text: Color,
    val muted: Color,
    val border: Color
)

private val LightCardPalette = CardPalette(
    surface = Color(0xFFFAFAFA),
    text = Color(0xFF111111),
    muted = Color(0xFF757575),
    border = Color(0xFFDCDCDC)
)

private val DarkCardPalette = CardPalette(
    surface = Color(0xFF141414),
    text = Color(0xFFF2F2F2),
    muted = Color(0xFF8A8A8A),
    border = Color(0xFF2E2E2E)
)

/**
 * Mostra a sequência atual de dias dentro do limite.
 *
 * @param days resultado de [calculateStreak]. Zero é tratado como um estado
 *        próprio ("ainda não começou"), não como um "0 day streak".
 */
@Composable
fun StreakCard(
    days: Int,
    modifier: Modifier = Modifier
) {
    val palette = if (isSystemInDarkTheme()) DarkCardPalette else LightCardPalette

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, palette.border),
        tonalElevation = 0.dp,  // sem sombra, igual ao resto do app
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // O emoji é o único elemento com cor na tela. Fica apagado
            // enquanto não há sequência — o próprio ícone vira o indicador.
            Text(
                text = if (days > 0) "🔥" else "·",
                fontSize = 22.sp
            )

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = streakLabel(days),
                    color = palette.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.padding(top = 2.dp))
                Text(
                    text = if (days > 0) "days within your limits" else "start today",
                    color = palette.muted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/** "3 day streak", com o singular certo em 1 dia. */
private fun streakLabel(days: Int): String = when {
    days <= 0 -> "No streak yet"
    days == 1 -> "1 day streak"
    else -> "$days day streak"
}

// --- Previews: abra o painel Split/Design no Android Studio para ver. ---

@Preview(name = "Streak 3", showBackground = true)
@Composable
private fun StreakCardPreview() {
    StreakCard(days = 3, modifier = Modifier.padding(16.dp))
}

@Preview(name = "Streak 1", showBackground = true)
@Composable
private fun StreakCardSingularPreview() {
    StreakCard(days = 1, modifier = Modifier.padding(16.dp))
}

@Preview(name = "Sem streak", showBackground = true)
@Composable
private fun StreakCardEmptyPreview() {
    StreakCard(days = 0, modifier = Modifier.padding(16.dp))
}
