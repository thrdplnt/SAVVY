package com.example.savvy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.savvy.ui.auth.LoginScreen
import com.example.savvy.ui.auth.RegisterScreen
import com.example.savvy.ui.anggaran.AnggaranScreen
import com.example.savvy.ui.home.HomeScreen
import com.example.savvy.ui.onboarding.OnboardingScreen
import com.example.savvy.ui.profile.EditProfileScreen
import com.example.savvy.ui.profile.ProfileScreen
import com.example.savvy.ui.profile.UbahKataSandi
import com.example.savvy.ui.riwayat.RiwayatScreen
import com.example.savvy.ui.splash.SplashScreen
import com.example.savvy.ui.tambah.TambahTransaksiScreen
import com.example.savvy.ui.tambah.TambahTransaksiViewModel
import com.example.savvy.ui.riwayat.CategoryDetailScreen
import androidx.navigation.NavType
import com.example.savvy.data.Screen
import com.example.savvy.ui.riwayat.DetailTransaksiScreen
import com.example.savvy.ui.wallet.DompetkuScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onClearSearch: ClearSearchCallback = {}
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
        composable(
            route = "${Screen.Onboarding.route}?initialPage={initialPage}",
            arguments = listOf(
                navArgument("initialPage") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0
            OnboardingScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                initialPage = initialPage
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
                viewModel = viewModel // Kirim viewModel jika diperlukan, atau biarkan default
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
        composable(Screen.Dompetku.route) {
            DompetkuScreen(navController = navController)
        }
        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            CategoryDetailScreen(navController = navController, category = category)
        }
        composable(Screen.ForgotPassword.route) {
            UbahKataSandi(navController = navController)
        }
        composable(
            route = Screen.DetailTransaksi.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            DetailTransaksiScreen(navController = navController, transactionId = transactionId)
        }
        composable(
            route = Screen.EditTransaksi.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val viewModel: TambahTransaksiViewModel = hiltViewModel()
            TambahTransaksiScreen(
                navController = navController,
                viewModel = viewModel, // Kirim viewModel jika diperlukan, atau biarkan default
                transactionId = transactionId
            )
        }
    }
}