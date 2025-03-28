package com.example.savvy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallet
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

@Composable
fun NavigationGraph() {
    val navController = rememberNavController()
    val bottomItems = listOf(
        Screen.Home to Icons.Default.Home,
        Screen.Riwayat to Icons.Default.Search,
        Screen.Tambah to Icons.Default.Add,
        Screen.Anggaran to Icons.Default.Wallet,
        Screen.Profile to Icons.Default.Person
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute !in listOf(
                    Screen.Splash.route,
                    Screen.Onboarding.route,
                    Screen.Register.route,
                    Screen.Login.route,
                    Screen.EditProfile.route
                )
            ) {
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
                            label = {
                                Text(
                                    when (screen) {
                                        Screen.Home -> "Home"
                                        Screen.Riwayat -> "Riwayat"
                                        Screen.Tambah -> "Tambah"
                                        Screen.Anggaran -> "Anggaran"
                                        Screen.Profile -> "Profile"
                                        else -> screen.route.replaceFirstChar { it.uppercase() }
                                    }
                                )
                            }
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