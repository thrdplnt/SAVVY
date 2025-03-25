package com.example.savvy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White

@Composable
fun SavvyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true // Tambahkan parameter enabled
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyLarge) }, // Inter Regular 16sp
        modifier = modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge, // Inter Regular 16sp untuk input
        enabled = enabled, // Gunakan parameter enabled
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Navy, // Navy
            unfocusedBorderColor = Navy, // Navy
            focusedContainerColor = White, // White
            unfocusedContainerColor = White, // White
            disabledBorderColor = Navy.copy(alpha = 0.3f), // Border saat dinonaktifkan
            disabledTextColor = Navy.copy(alpha = 0.6f), // Teks saat dinonaktifkan
            disabledLabelColor = Navy.copy(alpha = 0.6f) // Label saat dinonaktifkan
        )
    )
}