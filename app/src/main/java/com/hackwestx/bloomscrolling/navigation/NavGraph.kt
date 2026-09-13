package com.hackwestx.bloomscrolling.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hackwestx.bloomscrolling.ui.appselection.AppSelectionScreen
import com.hackwestx.bloomscrolling.ui.onboarding.OnboardingScreen
import com.hackwestx.bloomscrolling.ui.settings.SettingsScreen

/**
 * Nomes das rotas em um só lugar. Usar as constantes daqui (em vez de
 * digitar a String solta) evita erro de digitação silencioso na navegação.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val APP_SELECTION = "app_selection"
}

/**
 * Mapa de telas do app. Por enquanto cada rota é só um placeholder;
 * conforme cada tela real ficar pronta, troque o Placeholder(...)
 * pela Composable de verdade, ex.:
 *     composable(Routes.ONBOARDING) { OnboardingScreen(onAllGranted = { ... }) }
 */
@Composable
fun BloomNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.ONBOARDING
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onAllGranted = {
                    navController.navigate(Routes.APP_SELECTION) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) { PlaceholderScreen("Dashboard") }
        composable(Routes.SETTINGS) { SettingsScreen() }
        composable(Routes.APP_SELECTION) { AppSelectionScreen() }
    }
}

/** Tela temporária: só o nome, centralizado, para confirmar que a rota abriu. */
@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name, style = MaterialTheme.typography.headlineSmall)
    }
}
