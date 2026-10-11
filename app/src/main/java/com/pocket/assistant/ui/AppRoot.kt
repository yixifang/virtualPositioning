package com.pocket.assistant.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocket.assistant.location.ui.LocationScreen
import com.pocket.assistant.location.ui.MapPickerScreen
import com.pocket.assistant.process.ui.ProcessScreen
import com.pocket.assistant.process.ui.WhitelistScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        Tab("location", "虚拟定位", Icons.Filled.LocationOn),
        Tab("process", "手机瘦身", Icons.Filled.Bolt),
    )

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "location",
            modifier = Modifier.padding(padding),
        ) {
            composable("location") {
                LocationScreen(onOpenMap = { nav.navigate("map") })
            }
            composable("map") {
                MapPickerScreen(onBack = { nav.popBackStack() })
            }
            composable("process") {
                ProcessScreen(onOpenWhitelist = { nav.navigate("whitelist") })
            }
            composable("whitelist") {
                WhitelistScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
