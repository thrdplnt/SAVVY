package com.example.savvy.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savvy.R
import com.example.savvy.ui.auth.AuthViewModel
import com.example.savvy.ui.navigation.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    // State untuk melacak apakah user telah logout
    var hasLoggedOut by remember { mutableStateOf(false) }
    // State untuk menampilkan dialog konfirmasi logout
    var showDialog by remember { mutableStateOf(false) }

    // Ambil data user dari FirebaseAuth
    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Section
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Ganti dengan foto profil user
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tampilkan nama user dari Firebase, atau default ke "Nama User"
        Text(
            text = user?.displayName ?: "Nama User",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // Tampilkan email user dari Firebase, atau default ke "user@example.com"
        Text(
            text = user?.email ?: "user@example.com",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Akun Saya Button
        Button(
            onClick = { /* Navigasi ke halaman update profil */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Akun Saya")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Logout", color = Color.White)
        }

        // Dialog Konfirmasi Logout
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Konfirmasi Logout") },
                text = { Text("Apakah Anda yakin ingin logout?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.logout()
                            hasLoggedOut = true
                            showDialog = false
                        }
                    ) {
                        Text("Ya")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Tidak")
                    }
                }
            )
        }

        // Handle navigation after logout
        LaunchedEffect(hasLoggedOut) {
            if (hasLoggedOut) {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(0) { inclusive = true } // Clear semua back stack
                }
            }
        }
    }
}