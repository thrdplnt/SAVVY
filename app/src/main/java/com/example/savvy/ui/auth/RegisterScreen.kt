package com.example.savvy.ui.auth

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvy.R
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.data.Screen
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.ErrorRed
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) } // State untuk visibilitas password
    var isConfirmPasswordVisible by remember { mutableStateOf(false) } // State untuk visibilitas konfirmasi password

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
    ) {
        // Tombol panah kembali (kiri atas)
        IconButton(
            onClick = { navController.popBackStack() },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SIGN UP",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 411.dp)
                    .fillMaxWidth(0.77f)
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
                SavvyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama Lengkap"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SavvyTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    keyboardType = KeyboardType.Email
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

                Spacer(modifier = Modifier.height(16.dp))

                // Konfirmasi Password Field dengan ikon mata
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Konfirmasi Password") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isConfirmPasswordVisible) "Hide confirm password" else "Show confirm password",
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

                Spacer(modifier = Modifier.height(16.dp))

                authState.errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
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
                    text = "Create account",
                    onClick = {
                        viewModel.register(name, email, password, confirmPassword)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !authState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Navy)) {
                            append("Have an account? ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Navy,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Log in")
                        }
                    },
                    onClick = { offset: Int ->
                        val loginText = "Log in"
                        val loginStartIndex = "Have an account? ".length
                        val loginEndIndex = loginStartIndex + loginText.length
                        if (offset in loginStartIndex until loginEndIndex) {
                            navController.navigate(Screen.Login.route)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        }
    }
}