package com.hackwestx.bloomscrolling.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hackwestx.bloomscrolling.navigation.Routes
import com.hackwestx.bloomscrolling.ui.theme.ColorAccent
import com.hackwestx.bloomscrolling.ui.theme.ColorSurface
import com.hackwestx.bloomscrolling.ui.theme.ColorTextSecondary
import com.hackwestx.bloomscrolling.ui.theme.Label

private data class BottomNavTab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    BottomNavTab(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavTab(Routes.COMMUNITY, "Community", Icons.Filled.Groups),
    BottomNavTab(Routes.APP_SELECTION, "Apps", Icons.Filled.Apps),
    BottomNavTab(Routes.STREAK, "Streak", Icons.Filled.LocalFireDepartment)
)

@Composable
fun BloomBottomNav(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar(containerColor = ColorSurface) {
        TABS.forEach { tab ->
            val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        // Standard bottom-nav behavior: switching tabs never
                        // rebuilds the whole back stack, and re-tapping the
                        // current tab doesn't stack a duplicate destination.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(text = tab.label, style = Label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ColorAccent,
                    selectedTextColor = ColorAccent,
                    unselectedIconColor = ColorTextSecondary.copy(alpha = 0.5f),
                    unselectedTextColor = ColorTextSecondary.copy(alpha = 0.5f),
                    indicatorColor = ColorSurface
                )
            )
        }
    }
}
