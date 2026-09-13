package com.hackwestx.bloomscrolling.ui.appselection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackwestx.bloomscrolling.ui.theme.BodyLarge
import com.hackwestx.bloomscrolling.ui.theme.BodyRegular
import com.hackwestx.bloomscrolling.ui.theme.ColorAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorAccentSoft
import com.hackwestx.bloomscrolling.ui.theme.ColorBorder
import com.hackwestx.bloomscrolling.ui.theme.ColorTextOnAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorTextPrimary
import com.hackwestx.bloomscrolling.ui.theme.ColorTextSecondary
import com.hackwestx.bloomscrolling.ui.theme.HeadingH1
import com.hackwestx.bloomscrolling.ui.theme.Spacing

@Composable
fun AppSelectionScreen(
    viewModel: AppSelectionViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.lg)) {
        Text("Choose Apps", style = HeadingH1, color = ColorTextPrimary)
        Text(
            "Select which apps BloomScrolling limits",
            style = BodyRegular,
            color = ColorTextSecondary
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = Spacing.md)) {
            items(apps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    onToggle = { monitored -> viewModel.setMonitored(app.packageName, monitored) }
                )
                HorizontalDivider(color = ColorBorder)
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Text(
            text = app.appName,
            style = BodyLarge,
            color = ColorTextPrimary,
            modifier = Modifier
                .padding(start = Spacing.sm)
                .weight(1f)
        )
        Switch(
            checked = app.isMonitored,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorTextOnAccent,
                checkedTrackColor = ColorAccent,
                uncheckedTrackColor = ColorAccentSoft
            )
        )
    }
}
