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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.savvy.R
import com.example.savvy.data.Screen
import com.example.savvy.ui.auth.AuthViewModel
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.layout.ContentScale

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var hasLoggedOut by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Foto Profil
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Navy.copy(alpha = 0.1f))
        ) {
            if (user?.photoUrl != null) {
                AsyncImage(
                    model = user.photoUrl.toString(),
                    contentDescription = "Foto Profil",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop, // Zoom untuk mengisi lingkaran
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Foto Profil",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop // Zoom untuk mengisi lingkaran
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user?.displayName ?: "Nama Pengguna",
            style = MaterialTheme.typography.headlineLarge,
            color = Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 411.dp)
                .fillMaxWidth(0.77f)
        )

        Text(
            text = user?.email ?: "user@example.com",
            style = MaterialTheme.typography.labelSmall,
            color = Navy.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.89f)
                .background(White, shape = RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Beige, shape = RoundedCornerShape(8.dp))
                    .clickable { navController.navigate(Screen.EditProfile.route) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Edit Profil",
                        tint = Navy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Profil",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Panah",
                    tint = Navy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Beige, shape = RoundedCornerShape(8.dp))
                    .clickable { navController.navigate(Screen.ForgotPassword.route) }
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Panah",
                    tint = Navy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SavvyButton(
                text = "Logout",
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                textColor = White,
                backgroundColor = Navy
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

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

        LaunchedEffect(hasLoggedOut) {
            if (hasLoggedOut) {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}