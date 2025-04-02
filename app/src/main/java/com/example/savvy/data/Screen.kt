package com.example.savvy.data

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Register : Screen("register")
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Tambah : Screen("tambah")
    object Riwayat : Screen("riwayat")
    object Anggaran : Screen("anggaran")
    object EditProfile : Screen("edit_profile")
    object CategoryDetail : Screen("category_detail/{category}") {
        fun createRoute(category: String) = "category_detail/$category"
    }
}