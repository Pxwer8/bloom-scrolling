package com.hackwestx.bloomscrolling.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hackwestx.bloomscrolling.ui.theme.BodyLarge
import com.hackwestx.bloomscrolling.ui.theme.ColorAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorAccentSoft
import com.hackwestx.bloomscrolling.ui.theme.ColorTextOnAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorTextPrimary
import com.hackwestx.bloomscrolling.ui.theme.Radius
import com.hackwestx.bloomscrolling.ui.theme.Spacing

enum class BloomButtonStyle { Primary, Secondary }

@Composable
fun BloomButton(
    text: String,
    onClick: () -> Unit,
    style: BloomButtonStyle = BloomButtonStyle.Primary,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (style) {
        BloomButtonStyle.Primary -> ColorAccent to ColorTextOnAccent
        BloomButtonStyle.Secondary -> ColorAccentSoft to ColorTextPrimary
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.full),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(
            horizontal = Spacing.lg,
            vertical = Spacing.md
        ),
        modifier = modifier
    ) {
        Text(text = text, style = BodyLarge)
    }
}
