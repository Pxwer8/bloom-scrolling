package com.hackwestx.bloomscrolling.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackwestx.bloomscrolling.ui.components.AmountType
import com.hackwestx.bloomscrolling.ui.components.BloomCard
import com.hackwestx.bloomscrolling.ui.components.BloomListItem
import com.hackwestx.bloomscrolling.ui.theme.BodyRegular
import com.hackwestx.bloomscrolling.ui.theme.BodySmall
import com.hackwestx.bloomscrolling.ui.theme.ColorAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorAccentSoft
import com.hackwestx.bloomscrolling.ui.theme.ColorTextPrimary
import com.hackwestx.bloomscrolling.ui.theme.ColorTextSecondary
import com.hackwestx.bloomscrolling.ui.theme.HeadingH1
import com.hackwestx.bloomscrolling.ui.theme.HeadingH2
import com.hackwestx.bloomscrolling.ui.theme.Spacing

// No user profile/auth system exists anywhere in the schema — hardcoded
// until one does, same category as the other placeholder values here.
private const val PLACEHOLDER_USER_NAME = "Alex"

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.lg)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome back,", style = BodyRegular, color = ColorTextSecondary)
                    Text(PLACEHOLDER_USER_NAME, style = HeadingH1, color = ColorTextPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ColorAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = ColorAccent)
                }
            }
        }

        item {
            Spacer(Modifier.height(Spacing.xl))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.md),
                contentAlignment = Alignment.Center
            ) {
                ProgressRing(percent = state.summary.percentSavedThisMonth)
            }
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatBlock(
                    value = state.summary.daysActive.toString(),
                    label = "Days Active",
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    value = "%.1f".format(state.summary.weeksSaved),
                    label = "Weeks Saved",
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    value = state.summary.projectedHoursPerYear.toString(),
                    label = "Hours/Year Projected",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(Modifier.height(Spacing.xl))
            Text("This Week", style = HeadingH2, color = ColorTextPrimary)
            Spacer(Modifier.height(Spacing.xs))
        }

        if (state.recentEntries.isEmpty()) {
            item {
                Text(
                    "No monitored apps yet — head to the Apps tab to get started.",
                    style = BodySmall,
                    color = ColorTextSecondary
                )
            }
        } else {
            items(state.recentEntries) { entry ->
                BloomListItem(
                    leadingIcon = Icons.Filled.LocalFireDepartment,
                    title = "${entry.appName} avoided",
                    subtitle = entry.subtitle,
                    amountText = "+${entry.minutesSaved} min",
                    amountType = AmountType.Positive
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(percent: Int) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            drawArc(
                color = ColorAccentSoft,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = ColorAccent,
                startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(text = "$percent%", style = HeadingH1, color = ColorTextPrimary)
    }
}

@Composable
private fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    BloomCard(modifier = modifier) {
        Text(text = value, style = HeadingH2, color = ColorTextPrimary)
        Text(text = label, style = BodySmall, color = ColorTextSecondary)
    }
}
