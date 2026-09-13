package com.hackwestx.bloomscrolling.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hackwestx.bloomscrolling.ui.components.AmountType
import com.hackwestx.bloomscrolling.ui.components.BloomCard
import com.hackwestx.bloomscrolling.ui.components.BloomListItem
import com.hackwestx.bloomscrolling.ui.theme.BodyRegular
import com.hackwestx.bloomscrolling.ui.theme.ColorAccentSoft
import com.hackwestx.bloomscrolling.ui.theme.ColorTextPrimary
import com.hackwestx.bloomscrolling.ui.theme.ColorTextSecondary
import com.hackwestx.bloomscrolling.ui.theme.HeadingH1
import com.hackwestx.bloomscrolling.ui.theme.Radius
import com.hackwestx.bloomscrolling.ui.theme.Spacing

// ---------------------------------------------------------------------------
// DEMO DATA ONLY. There is no friends/social system anywhere in the schema
// or the team's plan — this screen is visual polish, not wired to anything
// real. Do not treat these names/numbers as real user data.
// ---------------------------------------------------------------------------

private data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val timeSaved: String,
    val isCurrentUser: Boolean
)

private val DEMO_LEADERBOARD = listOf(
    LeaderboardEntry(1, "Priya", "4h 05m", isCurrentUser = false),
    LeaderboardEntry(2, "You", "3h 20m", isCurrentUser = true),
    LeaderboardEntry(3, "Marcus", "2h 58m", isCurrentUser = false),
    LeaderboardEntry(4, "Wei", "2h 40m", isCurrentUser = false),
    LeaderboardEntry(5, "Sofia", "2h 12m", isCurrentUser = false),
    LeaderboardEntry(6, "Diego", "1h 50m", isCurrentUser = false),
    LeaderboardEntry(7, "Amara", "1h 22m", isCurrentUser = false),
    LeaderboardEntry(8, "Liam", "0h 58m", isCurrentUser = false)
)

private val CURRENT_USER_RANK = DEMO_LEADERBOARD.first { it.isCurrentUser }.rank

@Composable
fun CommunityScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.lg)
    ) {
        item {
            Text("Community", style = HeadingH1, color = ColorTextPrimary)
            Text(
                "Compare your progress with friends",
                style = BodyRegular,
                color = ColorTextSecondary
            )
        }

        item {
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(top = Spacing.lg)
            )
            BloomCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "You're #$CURRENT_USER_RANK of ${DEMO_LEADERBOARD.size} friends this week",
                    style = BodyRegular,
                    color = ColorTextPrimary
                )
            }
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(top = Spacing.md)
            )
        }

        items(DEMO_LEADERBOARD, key = { it.rank }) { entry ->
            val rowModifier = if (entry.isCurrentUser) {
                Modifier
                    .fillMaxWidth()
                    .background(ColorAccentSoft, RoundedCornerShape(Radius.md))
                    .padding(horizontal = Spacing.sm)
            } else {
                Modifier.fillMaxWidth()
            }

            BloomListItem(
                leadingIcon = Icons.Filled.Person,
                title = "#${entry.rank} ${entry.name}" + if (entry.isCurrentUser) " · You" else "",
                subtitle = "This week",
                amountText = entry.timeSaved,
                amountType = AmountType.Positive,
                modifier = rowModifier
            )
        }
    }
}
