package com.example.savvy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.savvy.ui.auth.LoginScreen
import com.example.savvy.ui.auth.RegisterScreen
import com.example.savvy.ui.bookmark.BookmarkScreen
import com.example.savvy.ui.home.HomeScreen
import com.example.savvy.ui.onboarding.OnboardingScreen
import com.example.savvy.ui.profile.ProfileScreen
import com.example.savvy.ui.search.SearchScreen
import com.example.savvy.ui.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Register : Screen("register")
    object Login : Screen("login")
    object Home : Screen("home")
    object Search : Screen("search")
    object Bookmark : Screen("bookmark")
    object Profile : Screen("profile")
}

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
            HomeScreen(onAnimeClick = { })
        }
        composable(Screen.Search.route) {
            SearchScreen(onAnimeClick = { })
        }
        composable(Screen.Bookmark.route) {
            BookmarkScreen(onAnimeClick = { })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
    }
}