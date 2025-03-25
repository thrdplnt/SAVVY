package com.example.savvy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy

@Composable
fun SavvyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = Navy, // Default ke Navy
    backgroundColor: Color = Beige // Default ke Beige
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(300.dp)
            .height(48.11.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(24.dp)
            ),
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = textColor.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium, // Inter Medium 16sp
            color = textColor
        )
    }
}