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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppSelectionScreen(
    viewModel: AppSelectionViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Choose which apps to monitor",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    onToggle = { monitored -> viewModel.setMonitored(app.packageName, monitored) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = app.appName,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        )
        Switch(
            checked = app.isMonitored,
            onCheckedChange = onToggle
        )
    }
}
