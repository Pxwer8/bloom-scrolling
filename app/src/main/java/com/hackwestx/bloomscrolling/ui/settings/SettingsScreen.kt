package com.hackwestx.bloomscrolling.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val surveyFrequency by viewModel.surveyFrequencyMinutes.collectAsState()
    val filterIntensity by viewModel.filterIntensity.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        item {
            LabeledSlider(
                label = "Survey frequency",
                valueText = "every $surveyFrequency min",
                value = surveyFrequency.toFloat(),
                valueRange = 1f..30f,
                onValueChange = { viewModel.setSurveyFrequency(it.roundToInt()) }
            )
        }

        item {
            LabeledSlider(
                label = "Filter intensity",
                valueText = "${(filterIntensity * 100).roundToInt()}%",
                value = filterIntensity,
                valueRange = 0f..1f,
                onValueChange = { viewModel.setFilterIntensity(it) }
            )
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Daily limits", style = MaterialTheme.typography.titleMedium)
            Text(
                "Only apps you're monitoring in App Selection show up here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        items(monitoredApps, key = { it.packageName }) { app ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                LabeledSlider(
                    label = app.appName,
                    valueText = "${app.dailyLimitMinutes} min/day",
                    value = app.dailyLimitMinutes.toFloat(),
                    valueRange = 5f..180f,
                    onValueChange = { viewModel.setDailyLimit(app.packageName, it.roundToInt()) }
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            valueText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}
