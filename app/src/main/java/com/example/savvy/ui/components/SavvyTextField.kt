package com.example.savvy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    enabled: Boolean = true,
    readOnly: Boolean = false, // Parameter baru
    onClickAction: (() -> Unit)? = null, // Parameter baru
    isPassword: Boolean = false, // Parameter lama Anda
    keyboardType: KeyboardType = KeyboardType.Text, // Parameter lama Anda
    visualTransformation: VisualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None, // Disesuaikan
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge.copy(color = Navy)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Panggil onClickAction ketika field ditekan dan readOnly=true
    LaunchedEffect(isPressed) {
        if (isPressed && readOnly && onClickAction != null) {
            onClickAction()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyLarge.copy(color = Navy.copy(alpha = 0.7f))) },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (readOnly && onClickAction != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null // Tidak ada ripple jika hanya untuk trigger klik
                    ) {
                        // Aksi klik sudah ditangani oleh LaunchedEffect,
                        // atau bisa juga dipanggil di sini jika mau: onClickAction()
                    }
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        readOnly = readOnly, // Gunakan parameter readOnly di sini
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = singleLine,
        textStyle = textStyle,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Navy,
            unfocusedBorderColor = Navy.copy(alpha = 0.7f),
            disabledBorderColor = Navy.copy(alpha = 0.3f),
            cursorColor = Navy,
            focusedTextColor = Navy,
            unfocusedTextColor = Navy,
            disabledTextColor = Navy.copy(alpha = 0.7f), // Warna teks saat disabled
            focusedLabelColor = Navy,
            unfocusedLabelColor = Navy.copy(alpha = 0.7f),
            disabledLabelColor = Navy.copy(alpha = 0.3f), // Warna label saat disabled
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            disabledContainerColor = White.copy(alpha = 0.5f) // Background saat disabled
        )
    )
}