package com.example.savvy.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvy.R
import com.example.savvy.ui.components.SavvyButton // Tambahkan impor ini
import com.example.savvy.ui.navigation.Screen
import com.example.savvy.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Deklarasi email dan password di scope yang lebih tinggi
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige) // Latar belakang beige
    ) {
        // Tombol panah kembali (kiri atas)
        IconButton(
            onClick = {
                // Kembali ke layar sebelumnya dalam stack navigasi
                navController.popBackStack()
            },
            modifier = Modifier
                .absoluteOffset(x = 16.dp, y = 42.dp) // Posisi absolut: x = 16 dp, y = 42 dp
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Navy
            )
        }

        // Logo Savvy (kanan atas)
        Image(
            painter = painterResource(id = R.drawable.logo_savvy_small), // Logo kecil
            contentDescription = "Savvy Logo",
            modifier = Modifier
                .absoluteOffset(x = (-42).dp, y = 42.dp) // Posisi absolut: x dari kanan, y = 42 dp
                .size(120.dp, 40.dp)
                .align(Alignment.TopEnd)
        )

        // Konten utama (judul, field, tombol, teks)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Judul "SIGN IN"
            Text(
                text = "SIGN IN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 411.dp) // Batasi lebar maksimum
                    .fillMaxWidth(0.77f) // 77% dari lebar layar
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Kontainer untuk field, tombol, dan teks
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp) // Batasi lebar maksimum kontainer
                    .fillMaxWidth(0.89f) // 89% dari lebar layar
                    .background(White, shape = RoundedCornerShape(8.dp)) // Background putih dengan sudut membulat
                    .padding(16.dp), // Padding internal di dalam kontainer
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth(), // Mengisi lebar penuh kontainer
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp), // Sudut membulat
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Navy,
                        focusedContainerColor = White, // Background putih saat fokus
                        unfocusedContainerColor = White // Background putih saat tidak fokus
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth(), // Mengisi lebar penuh kontainer
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp), // Sudut membulat
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Navy,
                        focusedContainerColor = White, // Background putih saat fokus
                        unfocusedContainerColor = White // Background putih saat tidak fokus
                    )
                )

                // Error Message (diletakkan langsung di bawah Password Field)
                authState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(6.dp)) // Jarak 8.dp dari Password Field
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Loading Indicator
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        color = Navy,
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // Tambahkan Spacer jika tidak ada Loading Indicator untuk menjaga jarak
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Login Button (Menggunakan SavvyButton)
                SavvyButton(
                    text = "Log In",
                    onClick = {
                        viewModel.login(email, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !authState.isLoading,
                    textColor = Navy,
                    backgroundColor = Beige
                )

                Spacer(modifier = Modifier.height(16.dp)) // Jarak lebih besar antara tombol dan teks

                // Teks "Don't have an account? Sign in"
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Navy)) {
                            append("Don't have an account? ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Navy,
                                textDecoration = TextDecoration.Underline // Garis bawah untuk menunjukkan dapat diklik
                            )
                        ) {
                            append("Sign in")
                        }
                    },
                    onClick = { offset: Int ->
                        // Hanya bagian "Sign in" yang akan memicu navigasi ke Register
                        val signInText = "Sign in"
                        val signInStartIndex = "Don't have an account? ".length
                        val signInEndIndex = signInStartIndex + signInText.length
                        if (offset in signInStartIndex until signInEndIndex) {
                            navController.navigate(Screen.Register.route)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp), // Padding lebih kecil
                    style = LocalTextStyle.current.copy(
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(2.dp)) // Jarak kecil antara teks

                // Teks "Lupa Password?" (di dalam kontainer)
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Navy,
                                textDecoration = TextDecoration.Underline // Garis bawah untuk menunjukkan dapat diklik
                            )
                        ) {
                            append("Lupa Password?")
                        }
                    },
                    onClick = { _: Int ->
                        if (email.isNotEmpty()) {
                            viewModel.resetPassword(email)
                        } else {
                            Toast.makeText(context, "Masukkan email terlebih dahulu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp), // Padding lebih kecil
                    style = LocalTextStyle.current.copy(
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Handle navigation when success
            LaunchedEffect(authState.isSuccess) {
                if (authState.isSuccess) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        }
    }
}