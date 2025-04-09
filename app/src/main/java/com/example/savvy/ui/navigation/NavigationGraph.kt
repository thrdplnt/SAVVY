package com.example.savvy.ui.navigation

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.savvy.data.Screen
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.Beige
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.delay

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

    // State untuk melacak apakah splash screen benar-benar selesai
    var isSplashFinished by remember { mutableStateOf(false) }

    // Ambil rute saat ini
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Gunakan LaunchedEffect untuk menandai bahwa splash screen selesai setelah durasi minimum
    LaunchedEffect(Unit) {
        // Tambahkan penundaan minimum untuk memastikan splash screen selesai
        delay(2000L) // Tunggu 2 detik (sesuaikan dengan durasi splash screen Anda)

        // Setelah penundaan, periksa rute saat ini
        if (currentRoute !in listOf(
                Screen.Splash.route,
                Screen.Onboarding.route,
                Screen.Register.route,
                Screen.Login.route,
                Screen.EditProfile.route
            )
        ) {
            isSplashFinished = true
        }
    }

    // Pantau perubahan rute untuk memastikan isSplashFinished diatur dengan benar
    LaunchedEffect(currentRoute) {
        if (currentRoute !in listOf(
                Screen.Splash.route,
                Screen.Onboarding.route,
                Screen.Register.route,
                Screen.Login.route,
                Screen.EditProfile.route
            )
        ) {
            isSplashFinished = true
        }
    }

    Scaffold(
        bottomBar = {
            // Tampilkan navbar hanya jika splash screen sudah selesai dan rute saat ini bukan layar yang dikecualikan
            if (isSplashFinished && currentRoute != null && currentRoute !in listOf(
                    Screen.Splash.route,
                    Screen.Onboarding.route,
                    Screen.Register.route,
                    Screen.Login.route,
                    Screen.EditProfile.route
                )
            ) {
                NavigationBar(
                    containerColor = Beige, // Warna beige dari ui.theme
                    tonalElevation = 0.dp // Hapus bayangan default
                ) {
                    bottomItems.forEach { (screen, icon) ->
                        val isSelected = currentRoute == screen.route
                        // Animasi warna untuk ikon dan teks
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) Navy else Color.Gray,
                            label = "iconColor"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Navy else Color.Gray,
                            label = "textColor"
                        )

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.route,
                                    tint = iconColor
                                    // Tidak menggunakan Modifier.size(), biarkan ukuran default
                                )
                            },
                            label = {
                                Text(
                                    text = when (screen) {
                                        Screen.Home -> "Home"
                                        Screen.Riwayat -> "Riwayat"
                                        Screen.Tambah -> "Tambah"
                                        Screen.Anggaran -> "Anggaran"
                                        Screen.Profile -> "Profile"
                                        else -> screen.route.replaceFirstChar { it.uppercase() }
                                    },
                                    color = textColor,
                                    fontSize = 12.sp, // Ukuran teks lebih kecil
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Navy,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = Navy,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Navy.copy(alpha = 0.2f) // Kurangi intensitas warna indikator menjadi 20%
                            )
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