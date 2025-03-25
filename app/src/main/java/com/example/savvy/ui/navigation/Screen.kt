package com.example.savvy.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Register : Screen("register")
    object Login : Screen("login")
    object Home : Screen("home")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Bookmark : Screen("bookmark")
    object Tambah : Screen("tambah")
    object Riwayat : Screen("riwayat")
    object Anggaran : Screen("anggaran")
    object AnimeDetail : Screen("anime_detail")
}