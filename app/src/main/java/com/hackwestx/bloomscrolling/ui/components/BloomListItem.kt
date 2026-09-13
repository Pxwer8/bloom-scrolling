package com.hackwestx.bloomscrolling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackwestx.bloomscrolling.ui.theme.BodyLarge
import com.hackwestx.bloomscrolling.ui.theme.BodySmall
import com.hackwestx.bloomscrolling.ui.theme.ColorAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorAccentSoft
import com.hackwestx.bloomscrolling.ui.theme.ColorNegative
import com.hackwestx.bloomscrolling.ui.theme.ColorPositive
import com.hackwestx.bloomscrolling.ui.theme.ColorTextPrimary
import com.hackwestx.bloomscrolling.ui.theme.ColorTextSecondary
import com.hackwestx.bloomscrolling.ui.theme.Spacing

enum class AmountType { Positive, Negative, Neutral }

@Composable
fun BloomListItem(
    leadingIcon: ImageVector,
    title: String,
    subtitle: String,
    amountText: String,
    amountType: AmountType,
    modifier: Modifier = Modifier
) {
    val amountColor = when (amountType) {
        AmountType.Positive -> ColorPositive
        AmountType.Negative -> ColorNegative
        AmountType.Neutral -> ColorTextPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(ColorAccentSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = ColorAccent)
        }

        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.sm)
                .weight(1f)
        ) {
            Text(text = title, style = BodyLarge, color = ColorTextPrimary)
            Text(text = subtitle, style = BodySmall, color = ColorTextSecondary)
        }

        Text(
            text = amountText,
            style = BodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}
