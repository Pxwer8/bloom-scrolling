package com.hackwestx.bloomscrolling.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hackwestx.bloomscrolling.ui.appselection.AppSelectionScreen
import com.hackwestx.bloomscrolling.ui.components.BloomBottomNav
import com.hackwestx.bloomscrolling.ui.home.HomeScreen
import com.hackwestx.bloomscrolling.ui.onboarding.OnboardingScreen
import com.hackwestx.bloomscrolling.ui.screens.CommunityScreen
import com.hackwestx.bloomscrolling.ui.screens.SplashScreen
import com.hackwestx.bloomscrolling.ui.settings.SettingsScreen
import com.hackwestx.bloomscrolling.ui.streak.StreakScreen

/**
 * Nomes das rotas em um só lugar. Usar as constantes daqui (em vez de
 * digitar a String solta) evita erro de digitação silencioso na navegação.
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"

    // Tabs inside the bottom-nav shell (MAIN).
    const val HOME = "home"
    const val COMMUNITY = "community"
    const val APP_SELECTION = "app_selection"
    const val STREAK = "streak"
}

@Composable
fun BloomNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onAllGranted = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) { MainShell() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}

/**
 * Bottom-nav shell: Home / Community / Apps / Streak share one Scaffold with
 * BloomBottomNav, in their own nested NavHost so tab switches don't touch the
 * outer graph's back stack (Splash/Onboarding/Settings).
 */
@Composable
private fun MainShell() {
    val innerNavController = rememberNavController()

    Scaffold(bottomBar = { BloomBottomNav(innerNavController) }) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.COMMUNITY) { CommunityScreen() }
            composable(Routes.APP_SELECTION) { AppSelectionScreen() }
            composable(Routes.STREAK) { StreakScreen() }
        }
    }
}

/** Tela temporária: só o nome, centralizado, para confirmar que a rota abriu. */
@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name, style = MaterialTheme.typography.headlineSmall)
    }
}
