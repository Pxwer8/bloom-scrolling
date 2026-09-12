package com.hackwestx.bloomscrolling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.hackwestx.bloomscrolling.navigation.BloomNavGraph
import com.hackwestx.bloomscrolling.ui.theme.BloomScrollingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloomScrollingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Toda a navegação do app vive no NavGraph agora.
                    // innerPadding evita que o conteúdo fique embaixo da
                    // status bar / navigation bar por causa do edge-to-edge.
                    BloomNavGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
