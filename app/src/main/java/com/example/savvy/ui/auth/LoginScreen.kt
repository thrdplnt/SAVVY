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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvy.R
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.data.Screen
import com.example.savvy.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Deklarasi email, password, dan state untuk visibilitas password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) } // State untuk visibilitas password

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
    ) {
        // Tombol panah kembali (kiri atas)
        IconButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier
                .absoluteOffset(x = 16.dp, y = 42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Navy
            )
        }

        // Logo Savvy (kanan atas)
        Image(
            painter = painterResource(id = R.drawable.logo_savvy_small),
            contentDescription = "Savvy Logo",
            modifier = Modifier
                .absoluteOffset(x = (-42).dp, y = 42.dp)
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
                    .widthIn(max = 411.dp)
                    .fillMaxWidth(0.77f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Kontainer untuk field, tombol, dan teks
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(0.89f)
                    .background(White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Navy,
                        focusedContainerColor = White,
                        unfocusedContainerColor = White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field dengan ikon mata
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = Navy
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Navy,
                        focusedContainerColor = White,
                        unfocusedContainerColor = White
                    )
                )

                // Error Message (diletakkan langsung di bawah Password Field)
                authState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(16.dp))

                // Teks "Don't have an account? Sign in"
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Navy)) {
                            append("Don't have an account? ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Navy,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Sign in")
                        }
                    },
                    onClick = { offset: Int ->
                        val signInText = "Sign in"
                        val signInStartIndex = "Don't have an account? ".length
                        val signInEndIndex = signInStartIndex + signInText.length
                        if (offset in signInStartIndex until signInEndIndex) {
                            navController.navigate(Screen.Register.route)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                    style = LocalTextStyle.current.copy(
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Teks "Lupa Password?" (di dalam kontainer)
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Navy,
                                textDecoration = TextDecoration.Underline
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
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
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