package com.example.savvy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.savvy.ui.auth.LoginScreen
import com.example.savvy.ui.auth.RegisterScreen
import com.example.savvy.ui.anggaran.AnggaranScreen
import com.example.savvy.ui.home.HomeScreen
import com.example.savvy.ui.onboarding.OnboardingScreen
import com.example.savvy.ui.profile.EditProfileScreen
import com.example.savvy.ui.profile.ProfileScreen
import com.example.savvy.ui.riwayat.RiwayatScreen
import com.example.savvy.ui.splash.SplashScreen
import com.example.savvy.ui.tambah.TambahTransaksiScreen
import com.example.savvy.ui.tambah.TambahTransaksiViewModel
import com.example.savvy.ui.riwayat.CategoryDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument


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
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
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
        composable(Screen.Riwayat.route) {
            RiwayatScreen(navController = navController)
        }
        composable(Screen.Tambah.route) {
            val viewModel: TambahTransaksiViewModel = hiltViewModel()
            TambahTransaksiScreen(
                navController = navController,
                uploader = viewModel.uploader
            )
        }
        composable(Screen.Anggaran.route) {
            AnggaranScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }
        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            CategoryDetailScreen(navController = navController, category = category)
        }
    }
}