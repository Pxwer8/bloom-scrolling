package com.hackwestx.bloomscrolling.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hackwestx.bloomscrolling.R
import com.hackwestx.bloomscrolling.ui.theme.ColorBg
import com.hackwestx.bloomscrolling.ui.theme.ColorSplashBg
import com.hackwestx.bloomscrolling.ui.theme.ColorTextOnAccent
import com.hackwestx.bloomscrolling.ui.theme.Display
import kotlinx.coroutines.delay

private const val FADE_IN_MS = 450
private const val HOLD_MS = 1200
private const val BG_TRANSITION_MS = 500

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var wordmarkVisible by remember { mutableStateOf(false) }
    var transitionToBg by remember { mutableStateOf(false) }

    val wordmarkAlpha by animateFloatAsState(
        targetValue = if (wordmarkVisible) 1f else 0f,
        animationSpec = tween(FADE_IN_MS),
        label = "wordmarkAlpha"
    )
    val wordmarkScale by animateFloatAsState(
        targetValue = if (wordmarkVisible) 1f else 0.8f,
        animationSpec = tween(FADE_IN_MS),
        label = "wordmarkScale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (transitionToBg) ColorBg else ColorSplashBg,
        animationSpec = tween(BG_TRANSITION_MS),
        label = "splashBackground"
    )

    LaunchedEffect(Unit) {
        wordmarkVisible = true
        delay((FADE_IN_MS + HOLD_MS).toLong())
        transitionToBg = true
        delay(BG_TRANSITION_MS.toLong())
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(wordmarkAlpha)
                .scale(wordmarkScale)
        ) {
            // Same bloom mark as the launcher icon, so the splash and the
            // home-screen icon read as one identity, not two unrelated marks.
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.height(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "BloomScrolling",
                style = Display,
                color = ColorTextOnAccent
            )
        }
    }
}
