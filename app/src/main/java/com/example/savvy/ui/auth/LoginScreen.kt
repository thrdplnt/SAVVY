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
import androidx.compose.ui.graphics.Color
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.savvy.R
import com.example.savvy.data.Screen
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // PERBAIKAN: Menggunakan Scaffold untuk struktur layout yang lebih baik
    Scaffold(
        containerColor = Beige, // Warna background utama
        topBar = {
            // PERBAIKAN: TopAppBar dibuat transparan agar menyatu dengan background
            TopAppBar(
                title = { /* Kosongkan agar tidak ada judul default */ },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Navy
                        )
                    }
                },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.logo_savvy_small),
                        contentDescription = "Savvy Logo",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(120.dp, 40.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // Membuat TopAppBar transparan
                )
            )
        }
    ) { paddingValues ->
        // Konten utama
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Judul "SIGN IN"
            Text(
                text = "SIGN IN",
                // PERBAIKAN: Menggunakan font dari theme
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 411.dp)
                    .fillMaxWidth(0.77f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Kontainer untuk form
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(0.89f)
                    // PERBAIKAN: Menggunakan warna dari theme
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    // PERBAIKAN: Menggunakan warna dari theme
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Navy.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
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
                    // PERBAIKAN: Menggunakan warna dari theme
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = Navy.copy(alpha = 0.7f)
                    )
                )

                authState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        // PERBAIKAN: Menggunakan font dari theme
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (authState.isLoading) {
                    CircularProgressIndicator(
                        color = Navy,
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                SavvyButton(
                    text = "Log In",
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !authState.isLoading,
                    textColor = Navy,
                    backgroundColor = Beige
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Teks "Don't have an account? Sign up"
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Navy)) { append("Don't have an account? ") }
                        withStyle(style = SpanStyle(color = Navy, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) { append("Sign up") }
                    },
                    onClick = { offset ->
                        val text = "Sign up"
                        val startIndex = "Don't have an account? ".length
                        if (offset in startIndex..(startIndex + text.length)) {
                            navController.navigate(Screen.Register.route)
                        }
                    },
                    modifier = Modifier.padding(2.dp),
                    // PERBAIKAN: Menggunakan font dari theme
                    style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Teks "Lupa Password?"
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Navy, textDecoration = TextDecoration.Underline)) { append("Lupa Password?") }
                    },
                    onClick = {
                        if (email.isNotEmpty()) {
                            viewModel.resetPassword(email)
                        } else {
                            Toast.makeText(context, "Masukkan email terlebih dahulu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(2.dp),
                    // PERBAIKAN: Menggunakan font dari theme
                    style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
}