package com.hackwestx.bloomscrolling.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackwestx.bloomscrolling.gamification.Achievement
import com.hackwestx.bloomscrolling.gamification.DayStatus
import com.hackwestx.bloomscrolling.ui.components.AmountType
import com.hackwestx.bloomscrolling.ui.components.BloomButton
import com.hackwestx.bloomscrolling.ui.components.BloomCard
import com.hackwestx.bloomscrolling.ui.components.BloomListItem
import com.hackwestx.bloomscrolling.ui.theme.BodySmall
import com.hackwestx.bloomscrolling.ui.theme.ColorAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorBorder
import com.hackwestx.bloomscrolling.ui.theme.ColorTextPrimary
import com.hackwestx.bloomscrolling.ui.theme.ColorTextSecondary
import com.hackwestx.bloomscrolling.ui.theme.Display
import com.hackwestx.bloomscrolling.ui.theme.HeadingH2
import com.hackwestx.bloomscrolling.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun StreakScreen(
    viewModel: StreakViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = ColorAccent,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(text = state.currentStreak.toString(), style = Display, color = ColorAccent)
            Text(
                text = if (state.currentStreak == 1) "day streak" else "day streak",
                style = HeadingH2,
                color = ColorTextSecondary
            )

            Spacer(Modifier.height(Spacing.xl))
            WeekStrip(statuses = state.weekStatuses)

            Spacer(Modifier.height(Spacing.xl))
            BloomCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = motivationalMessage(state.currentStreak, state.longestStreak),
                    style = BodySmall,
                    color = ColorTextPrimary
                )
            }

            Spacer(Modifier.height(Spacing.lg))
            Column(modifier = Modifier.fillMaxWidth()) {
                state.achievements.forEach { (achievement, unlocked) ->
                    BloomListItem(
                        leadingIcon = if (unlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                        title = achievement.title,
                        subtitle = achievement.description,
                        amountText = if (unlocked) "Unlocked" else "Locked",
                        amountType = if (unlocked) AmountType.Positive else AmountType.Neutral
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            BloomButton(
                text = "Share your streak",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Sharing isn't wired up yet — but here's your streak: ${state.currentStreak} days!"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeekStrip(statuses: List<DayStatus>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        statuses.forEach { status ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .then(
                            if (status.heldLimit) {
                                Modifier.background(ColorAccent, CircleShape)
                            } else {
                                Modifier.border(1.dp, ColorBorder, CircleShape)
                            }
                        )
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = status.date.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault()),
                    style = BodySmall,
                    color = ColorTextSecondary
                )
            }
        }
    }
}

private fun motivationalMessage(currentStreak: Int, longestStreak: Int): String = when {
    currentStreak == 0 && longestStreak == 0 -> "Start today to begin your streak!"
    currentStreak >= longestStreak && currentStreak > 0 -> "You're on your best streak yet — keep it going!"
    else -> {
        val remaining = longestStreak - currentStreak
        "Your longest streak was $longestStreak days — you're $remaining away from a new record!"
    }
}
