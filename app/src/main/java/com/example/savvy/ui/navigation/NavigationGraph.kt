
package com.example.savvy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.savvy.ui.navigation.Screen
import com.example.savvy.ui.tambah.TambahTransaksiScreen

@Composable
fun NavigationGraph() {
    val navController = rememberNavController()
    val bottomItems = listOf(
        Screen.Home to Icons.Default.Home,
        Screen.Riwayat to Icons.Default.Search, // Ganti "Search" menjadi "Riwayat"
        Screen.Tambah to Icons.Default.Add, // Ganti "Bookmark" menjadi "Tambah"
        Screen.Anggaran to Icons.Default.Search, // Ganti "Profile" menjadi "Anggaran"
        Screen.Profile to Icons.Default.Person
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute !in listOf(Screen.Splash.route, Screen.Onboarding.route, Screen.Register.route, Screen.Login.route)) {
                NavigationBar {
                    bottomItems.forEach { (screen, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = screen.route) },
                            label = { Text(screen.route.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(navController = navController, startDestination = Screen.Splash.route)
        }
    }
}