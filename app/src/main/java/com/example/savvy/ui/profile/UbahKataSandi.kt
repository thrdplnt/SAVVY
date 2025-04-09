package com.example.savvy.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UbahKataSandi(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Pastikan pengguna sudah login
    if (currentUser == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show()
            navController.popBackStack() // Kembali ke layar sebelumnya
        }
        return
    }

    // State untuk langkah-langkah
    var step by remember { mutableStateOf(1) } // 1: Verifikasi kata sandi lama, 2: Masukkan kata sandi baru
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
    ) {
        // Ikon Kembali
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

            // Judul
            Text(
                text = if (step == 1) "Verifikasi Identitas" else "Ubah Kata Sandi",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 411.dp)
                    .fillMaxWidth(0.77f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Konten Utama
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(0.89f)
                    .background(White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    1 -> {
                        // Langkah 1: Verifikasi Kata Sandi Lama
                        SavvyTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = "Masukkan Kata Sandi Lama",
                            modifier = Modifier.fillMaxWidth(),
                            isPassword = true // Gunakan isPassword untuk menyembunyikan input
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SavvyButton(
                            text = "Verifikasi",
                            onClick = {
                                if (oldPassword.isEmpty()) {
                                    Toast.makeText(context, "Masukkan kata sandi lama terlebih dahulu", Toast.LENGTH_SHORT).show()
                                    return@SavvyButton
                                }
                                isLoading = true

                                // Verifikasi kata sandi lama
                                val credential = EmailAuthProvider.getCredential(currentUser.email ?: "", oldPassword)
                                currentUser.reauthenticate(credential)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Verifikasi berhasil!", Toast.LENGTH_SHORT).show()
                                        step = 2 // Pindah ke langkah berikutnya
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Kata sandi lama salah: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            textColor = Navy,
                            backgroundColor = Beige,
                            enabled = !isLoading
                        )
                    }

                    2 -> {
                        // Langkah 2: Masukkan Kata Sandi Baru
                        SavvyTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "Kata Sandi Baru",
                            modifier = Modifier.fillMaxWidth(),
                            isPassword = true // Gunakan isPassword untuk menyembunyikan input
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SavvyTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Konfirmasi Kata Sandi",
                            modifier = Modifier.fillMaxWidth(),
                            isPassword = true // Gunakan isPassword untuk menyembunyikan input
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SavvyButton(
                            text = "Simpan Perubahan",
                            onClick = {
                                if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                                    Toast.makeText(context, "Masukkan kata sandi terlebih dahulu", Toast.LENGTH_SHORT).show()
                                    return@SavvyButton
                                }
                                if (newPassword != confirmPassword) {
                                    Toast.makeText(context, "Kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                                    return@SavvyButton
                                }
                                if (newPassword.length < 6) {
                                    Toast.makeText(context, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                                    return@SavvyButton
                                }
                                isLoading = true

                                // Ubah kata sandi
                                currentUser.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Kata sandi berhasil diubah!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack() // Kembali ke ProfileScreen
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Gagal mengubah kata sandi: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            textColor = Navy,
                            backgroundColor = Beige,
                            enabled = !isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Loading Indicator
        if (isLoading) {
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
}