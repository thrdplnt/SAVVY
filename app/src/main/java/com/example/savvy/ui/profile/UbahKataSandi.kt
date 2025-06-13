package com.example.savvy.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbahKataSandi(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        return
    }

    var step by remember { mutableStateOf(1) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Navy
                )
            }
            Text(
                text = if (step == 1) "Verifikasi Identitas" else "Ubah Kata Sandi",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Navy,
                modifier = Modifier.offset(x = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(0.89f)
                    // PERBAIKAN: Mengembalikan background menjadi White secara eksplisit
                    .background(White, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    1 -> {
                        SavvyTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = "Masukkan Kata Sandi Lama",
                            isPassword = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SavvyButton(
                            text = "Verifikasi",
                            onClick = {
                                if (oldPassword.isEmpty()) {
                                    Toast.makeText(context, "Masukkan kata sandi lama", Toast.LENGTH_SHORT).show()
                                    return@SavvyButton
                                }
                                isLoading = true
                                val credential = EmailAuthProvider.getCredential(currentUser.email ?: "", oldPassword)
                                currentUser.reauthenticate(credential)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Verifikasi berhasil!", Toast.LENGTH_SHORT).show()
                                        step = 2
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, "Kata sandi lama salah", Toast.LENGTH_LONG).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            textColor = Navy,
                            backgroundColor = Beige,
                            enabled = !isLoading
                        )
                    }

                    2 -> {
                        SavvyTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "Kata Sandi Baru",
                            isPassword = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SavvyTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Konfirmasi Kata Sandi",
                            isPassword = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SavvyButton(
                            text = "Simpan Perubahan",
                            onClick = {
                                if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                                    Toast.makeText(context, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
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
                                currentUser.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Kata sandi berhasil diubah!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Gagal mengubah kata sandi", Toast.LENGTH_LONG).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            textColor = Navy,
                            backgroundColor = Beige,
                            enabled = !isLoading
                        )
                    }
                }
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Navy)
        }
    }
}