package com.hackwestx.bloomscrolling.ui.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Single onboarding screen, three rows, each with its own Settings
// deep-link — as specced for Phase 2. Re-checks status via the button
// (add a Lifecycle observer later if you want it to refresh automatically
// on returning from Settings; not worth the extra plumbing on hour 2).

private data class PermissionRow(
    val title: String,
    val isGranted: (Context) -> Boolean,
    val settingsIntent: (Context) -> Intent
)

private val permissionRows = listOf(
    PermissionRow(
        title = "Usage access",
        isGranted = { ctx ->
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
            ) == AppOpsManager.MODE_ALLOWED
        },
        settingsIntent = { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
    ),
    PermissionRow(
        title = "Display over other apps",
        isGranted = { ctx -> Settings.canDrawOverlays(ctx) },
        settingsIntent = { ctx ->
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
        }
    ),
    PermissionRow(
        title = "Accessibility service",
        isGranted = { ctx ->
            val enabled = Settings.Secure.getString(
                ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            // Simplified substring check — fine for a hackathon; a shipped
            // app should parse the colon-separated list properly instead.
            enabled.contains("${ctx.packageName}/${ctx.packageName}.service.MonitorAccessibilityService")
        },
        settingsIntent = { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
    )
)

@Composable
fun OnboardingScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    val statuses = remember(refreshKey) { permissionRows.map { it.isGranted(context) } }

    LaunchedEffect(statuses) {
        if (statuses.all { it }) onAllGranted()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Beyond the Feed needs three permissions before it can work:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        permissionRows.forEachIndexed { index, row ->
            PermissionRowItem(
                title = row.title,
                granted = statuses[index],
                onClick = {
                    context.startActivity(
                        row.settingsIntent(context).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { refreshKey++ }, modifier = Modifier.fillMaxWidth()) {
            Text("I granted these — check again")
        }
    }
}

@Composable
private fun PermissionRowItem(title: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        if (granted) Text("Granted ✓") else TextButton(onClick = onClick) { Text("Grant") }
    }
}
