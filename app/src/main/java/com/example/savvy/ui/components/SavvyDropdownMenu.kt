package com.example.savvy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavvyDropdownMenu(
    label: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            label = { Text(label, style = MaterialTheme.typography.bodyLarge.copy(color = Navy.copy(alpha = 0.7f))) },
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    // PERBAIKAN: Menggunakan warna Navy agar konsisten
                    tint = Navy.copy(alpha = if (enabled) 1f else 0.5f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            // PERBAIKAN: Menggunakan font dari theme agar konsisten
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Navy),
            // PERBAIKAN: Mengganti seluruh blok warna agar sama dengan SavvyTextField
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Navy,
                unfocusedBorderColor = Navy.copy(alpha = 0.7f),
                disabledBorderColor = Navy.copy(alpha = 0.3f),
                cursorColor = Navy,
                focusedTextColor = Navy,
                unfocusedTextColor = Navy,
                disabledTextColor = Navy.copy(alpha = 0.7f),
                focusedLabelColor = Navy,
                unfocusedLabelColor = Navy.copy(alpha = 0.7f),
                disabledLabelColor = Navy.copy(alpha = 0.3f),
                // Ini bagian terpenting: background di-set menjadi putih
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                disabledContainerColor = White.copy(alpha = 0.5f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}