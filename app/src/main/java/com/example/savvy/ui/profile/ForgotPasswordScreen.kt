package com.example.savvy.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ForgotPasswordScreen(navController: NavController) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (step == 1) "Verifikasi Identitas" else "Ubah Kata Sandi",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(bottom = 32.dp)
        )

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

                Button(
                    onClick = {
                        if (oldPassword.isEmpty()) {
                            Toast.makeText(context, "Masukkan kata sandi lama terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@Button
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
                    enabled = !isLoading,
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Beige,
                        contentColor = Navy
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Navy,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Verifikasi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
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

                Button(
                    onClick = {
                        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                            Toast.makeText(context, "Masukkan kata sandi terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "Kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                            return@Button
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
                    enabled = !isLoading,
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Beige,
                        contentColor = Navy
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Navy,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Simpan Kata Sandi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}