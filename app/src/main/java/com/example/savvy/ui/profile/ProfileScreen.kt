package com.example.savvy.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvy.R
import com.example.savvy.ui.auth.AuthViewModel
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.navigation.Screen
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var hasLoggedOut by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(horizontal = 16.dp), // Margin horizontal 16dp
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp)) // Jarak atas 32dp, seperti versi sebelumnya

        // Foto Profil
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Navy.copy(alpha = 0.1f)) // Navy dengan opacity
        )

        Spacer(modifier = Modifier.height(16.dp)) // Jarak 16dp

        // Nama Pengguna
        Text(
            text = user?.displayName ?: "Nama User",
            style = MaterialTheme.typography.headlineLarge, // Poppins Bold 24sp
            color = Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 411.dp)
                .fillMaxWidth(0.77f)
        )

        // Email
        Text(
            text = user?.email ?: "user@example.com",
            style = MaterialTheme.typography.labelSmall, // Inter Regular 12sp
            color = Navy.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp)) // Jarak 32dp sebelum kontainer

        // Kontainer Putih
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.89f) // Lebar sama seperti RegisterScreen
                .background(White, shape = RoundedCornerShape(8.dp))
                .padding(16.dp), // Padding internal 16dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tombol Edit Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Beige, shape = RoundedCornerShape(8.dp))
                    .clickable { navController.navigate(Screen.EditProfile.route) } // Navigasi ke EditProfileScreen
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Edit Profile",
                        tint = Navy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.bodyMedium, // Inter Medium 16sp
                        color = Navy
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Arrow",
                    tint = Navy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // Jarak 16dp antar elemen

            // Tombol Lupa Password
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Beige, shape = RoundedCornerShape(8.dp))
                    .clickable { /* Navigasi ke halaman lupa password */ }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lupa Password",
                        tint = Navy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lupa Password",
                        style = MaterialTheme.typography.bodyMedium, // Inter Medium 16sp
                        color = Navy
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Arrow",
                    tint = Navy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp)) // Jarak 24dp sebelum tombol Logout

            // Tombol Logout
            SavvyButton(
                text = "Logout",
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                textColor = White, // Teks putih
                backgroundColor = Navy // Background Navy
            )
        }

        Spacer(modifier = Modifier.height(32.dp)) // Jarak bawah 32dp

        // Dialog Konfirmasi Logout
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        "Konfirmasi Logout",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Navy
                    )
                },
                text = {
                    Text(
                        "Apakah Anda yakin ingin logout?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy
                    )
                },
                confirmButton = {
                    SavvyButton(
                        text = "Ya",
                        onClick = {
                            viewModel.logout()
                            hasLoggedOut = true
                            showDialog = false
                        },
                        textColor = White,
                        backgroundColor = Navy
                    )
                },
                dismissButton = {
                    SavvyButton(
                        text = "Tidak",
                        onClick = { showDialog = false },
                        textColor = Navy,
                        backgroundColor = Beige
                    )
                },
                containerColor = White
            )
        }

        // Navigasi setelah logout
        LaunchedEffect(hasLoggedOut) {
            if (hasLoggedOut) {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}