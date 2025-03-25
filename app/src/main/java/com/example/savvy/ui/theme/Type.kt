package com.example.savvy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.savvy.R

// Definisikan FontFamily untuk Poppins
val Poppins = FontFamily(
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

// Definisikan FontFamily untuk Inter
val Inter = FontFamily(
    Font(R.font.inter_18pt_regular, FontWeight.Normal),
    Font(R.font.inter_18pt_medium, FontWeight.Medium)
)

// Definisikan Typography sesuai panduan
val Typography = Typography(
    // Heading 1: Poppins Bold, 24sp
    headlineLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 31.sp, // 130% dari 24sp
        letterSpacing = 0.sp
    ),
    // Heading 2: Poppins SemiBold, 22sp
    headlineMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp, // 130% dari 22sp
        letterSpacing = 0.sp
    ),
    // Normal: Inter Regular, 16sp
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp, // Meskipun fontnya inter18pt, kita atur ke 16sp
        lineHeight = 24.sp, // 150% dari 16sp
        letterSpacing = 0.5.sp
    ),
    // Normal Tebal: Inter Medium, 16sp
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp, // Meskipun fontnya inter18pt, kita atur ke 16sp
        lineHeight = 24.sp, // 150% dari 16sp
        letterSpacing = 0.5.sp
    ),
    // Caption: Inter Regular, 12sp
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp, // Meskipun fontnya inter18pt, kita atur ke 12sp
        lineHeight = 18.sp, // 150% dari 12sp
        letterSpacing = 0.5.sp
    )
)