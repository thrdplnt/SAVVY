package com.example.savvy.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.savvy.R
import com.example.savvy.data.Screen
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.FirebaseAuth

@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel() // Gunakan hiltViewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    val profileState by viewModel.profileState.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var profileImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var photoUrl by remember { mutableStateOf(user?.photoUrl?.toString()) }

    // Launcher untuk memilih gambar dari galeri
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            profileImageUri = it
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .absoluteOffset(x = 16.dp, y = 42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                tint = Navy
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    .clickable { pickImageLauncher.launch("image/*") }
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        placeholder = painterResource(id = R.drawable.ic_launcher_foreground)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Ganti Foto",
                    tint = Navy,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(White, shape = CircleShape)
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Judul
            Text(
                text = "Edit Profil",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 411.dp)
                    .fillMaxWidth(0.77f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Kontainer Putih
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(0.89f)
                    .background(White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nama Lengkap
                SavvyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama Lengkap"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email (tidak dapat diedit)
                SavvyTextField(
                    value = user?.email ?: "user@example.com",
                    onValueChange = { /* Tidak bisa diedit */ },
                    label = "Email",
                    enabled = false
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pesan Error (jika ada)
                profileState.errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Tombol Simpan Perubahan
                SavvyButton(
                    text = "Simpan Perubahan",
                    onClick = {
                        viewModel.updateProfile(name, profileImageUri)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    textColor = Navy,
                    backgroundColor = Beige,
                    enabled = name.isNotBlank() && !profileState.isLoading
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Indikator loading
        if (profileState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Navy,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    // Navigasi setelah berhasil memperbarui profil
    LaunchedEffect(profileState.isSuccess) {
        if (profileState.isSuccess) {
            Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            navController.popBackStack(Screen.Profile.route, inclusive = false)
        }
    }
}