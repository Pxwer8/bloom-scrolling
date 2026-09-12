package com.hackwestx.bloomscrolling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hackwestx.bloomscrolling.ui.onboarding.OnboardingScreen
import com.hackwestx.bloomscrolling.ui.theme.BloomScrollingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloomScrollingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var allGranted by remember { mutableStateOf(false) }
                    if (allGranted) {
                        Text(
                            text = "All permissions granted",
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        OnboardingScreen(
                            onAllGranted = { allGranted = true }
                        )
                    }
                }
            }
        }
    }
}
