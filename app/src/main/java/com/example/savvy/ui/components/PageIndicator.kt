package com.example.savvy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.savvy.ui.theme.Navy // Asumsi Navy adalah warna utama Anda
import com.example.savvy.ui.theme.SkyBlue // Asumsi SkyBlue atau warna lebih terang untuk titik tidak aktif

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    selectedColor: Color = Navy,        // Warna titik aktif
    unselectedColor: Color = SkyBlue,   // Warna titik tidak aktif
    dotSize: Dp = 8.dp,                 // Ukuran titik tidak aktif
    selectedDotSize: Dp = 10.dp,        // Ukuran titik aktif (bisa dibuat lebih besar)
    spacing: Dp = 8.dp                  // Jarak antar titik
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { iteration ->
            val isSelected = currentPage == iteration
            val size = if (isSelected) selectedDotSize else dotSize
            val color = if (isSelected) selectedColor else unselectedColor
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}