package dev.tomerklein.holocron.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.tomerklein.holocron.ui.about.AboutScreen
import dev.tomerklein.holocron.ui.destinations.DestinationEditScreen
import dev.tomerklein.holocron.ui.destinations.DestinationsScreen
import dev.tomerklein.holocron.ui.home.HomeScreen
import dev.tomerklein.holocron.ui.log.DeliveryLogScreen
import dev.tomerklein.holocron.ui.rules.RuleEditScreen
import dev.tomerklein.holocron.ui.rules.RulesScreen
import dev.tomerklein.holocron.ui.settings.SettingsScreen

/** Bottom-navigation tabs (the five screens from CLAUDE.md §7). */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Rules("rules", "Rules", Icons.Filled.Tune),
    Destinations("destinations", "Destinations", Icons.AutoMirrored.Filled.Send),
    DeliveryLog("log", "Log", Icons.AutoMirrored.Filled.List),
    Settings("settings", "Settings", Icons.Filled.Settings),
    About("about", "About", Icons.Filled.Info),
}

private object Routes {
    const val RULE_EDIT = "ruleEdit"
    const val DESTINATION_EDIT = "destinationEdit"
    fun ruleEdit(id: Long) = "$RULE_EDIT/$id"
    fun destinationEdit(id: Long) = "$DESTINATION_EDIT/$id"
}

@Composable
fun HolocronApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = backStackEntry?.destination
                    Tab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.Home.route) { HomeScreen() }

            composable(Tab.Rules.route) {
                RulesScreen(
                    onAddRule = { navController.navigate(Routes.ruleEdit(-1L)) },
                    onEditRule = { id -> navController.navigate(Routes.ruleEdit(id)) },
                )
            }
            composable(
                route = "${Routes.RULE_EDIT}/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.LongType }),
            ) {
                RuleEditScreen(onDone = { navController.popBackStack() })
            }

            composable(Tab.Destinations.route) {
                DestinationsScreen(
                    onAddDestination = { navController.navigate(Routes.destinationEdit(-1L)) },
                    onEditDestination = { id -> navController.navigate(Routes.destinationEdit(id)) },
                )
            }
            composable(
                route = "${Routes.DESTINATION_EDIT}/{destinationId}",
                arguments = listOf(navArgument("destinationId") { type = NavType.LongType }),
            ) {
                DestinationEditScreen(onDone = { navController.popBackStack() })
            }

            composable(Tab.DeliveryLog.route) { DeliveryLogScreen() }
            composable(Tab.Settings.route) { SettingsScreen() }
            composable(Tab.About.route) { AboutScreen() }
        }
    }
}
