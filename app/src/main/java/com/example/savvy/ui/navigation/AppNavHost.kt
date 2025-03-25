package com.example.savvy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.savvy.ui.anggaran.AnggaranScreen
import com.example.savvy.ui.tambah.TambahTransaksiScreen
import com.example.savvy.ui.auth.LoginScreen
import com.example.savvy.ui.auth.RegisterScreen
import com.example.savvy.ui.home.HomeScreen
import com.example.savvy.ui.onboarding.OnboardingScreen
import com.example.savvy.ui.profile.ProfileScreen
import com.example.savvy.ui.splash.SplashScreen
import com.example.savvy.ui.navigation.Screen
import com.example.savvy.ui.riwayat.RiwayatScreen
import com.example.savvy.ui.profile.EditProfileScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.Tambah.route) {
            TambahTransaksiScreen(navController = navController)
        }
        composable(Screen.Anggaran.route) {
            AnggaranScreen(navController = navController)
        }
        composable(Screen.Riwayat.route) {
            RiwayatScreen(navController = navController)
        }
        composable(Screen.EditProfile.route) { // Tambahkan rute ini
            EditProfileScreen(navController = navController)
        }
    }
}