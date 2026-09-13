package com.hackwestx.bloomscrolling.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hackwestx.bloomscrolling.ui.theme.ColorSurface
import com.hackwestx.bloomscrolling.ui.theme.HeadingH3
import com.hackwestx.bloomscrolling.ui.theme.Radius
import com.hackwestx.bloomscrolling.ui.theme.Spacing

@Composable
fun BloomCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = ColorSurface,
        shape = RoundedCornerShape(Radius.lg),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            if (title != null) {
                Text(text = title, style = HeadingH3)
            }
            content()
        }
    }
}
