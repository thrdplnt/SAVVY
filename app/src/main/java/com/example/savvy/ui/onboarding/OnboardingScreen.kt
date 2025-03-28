package com.example.savvy.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savvy.R
import com.example.savvy.ui.components.SavvyButton // Tambahkan impor ini
import com.example.savvy.ui.theme.*
import kotlinx.coroutines.launch

// Data class untuk menyimpan informasi slide
data class OnboardingSlideData(
    val illustrationResId: Int,
    val title: String,
    val description: String
)

// Komponen reusable untuk slide onboarding (Slide 1-3)
@Composable
fun OnboardingSlide(
    slideData: OnboardingSlideData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // Warna latar belakang putih
    ) {
        // Logo Savvy (posisi x = 42 dp, y = 42 dp dari kiri atas)
        Image(
            painter = painterResource(id = R.drawable.logo_savvy_small),
            contentDescription = "Savvy Logo",
            modifier = Modifier
                .absoluteOffset(x = 42.dp, y = 42.dp)
                .size(120.dp, 40.dp)
        )

        // Konten utama (ilustrasi, judul, deskripsi)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(255.dp)) // Gap antara logo dan ilustrasi

            // Ilustrasi (di tengah)
            Image(
                painter = painterResource(id = slideData.illustrationResId),
                contentDescription = "${slideData.title} Illustration",
                modifier = Modifier.size(300.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Judul
            Text(
                text = slideData.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Navy
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deskripsi
            Text(
                text = slideData.description,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Navy
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    // Data untuk masing-masing slide
    val slides = listOf(
        OnboardingSlideData(
            illustrationResId = R.drawable.onboarding_1,
            title = "Atur Keuanganmu dengan Savvy!",
            description = "Savvy membantu kamu mengelola keuangan pribadi dengan mudah. Mulai dari pencatatan hingga analisis, semua ada di sini!"
        ),
        OnboardingSlideData(
            illustrationResId = R.drawable.onboarding_2,
            title = "Catat dan Kelompokkan Pengeluaranmu!",
            description = "Pecahkan keuanganmu dengan fitur multi-dompet dan catat transaksi dengan mudah, manual atau melalui foto struk."
        ),
        OnboardingSlideData(
            illustrationResId = R.drawable.onboarding_3,
            title = "Pantau dan Rencanakan Keuanganmu!",
            description = "Lihat tren keuanganmu dengan grafik pie dan line, atur anggaran per kategori, dan ekspor laporan ke PDF kapan saja."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        // HorizontalPager untuk slide
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> OnboardingSlide(slides[0])
                1 -> OnboardingSlide(slides[1])
                2 -> OnboardingSlide(slides[2])
                3 -> OnboardingSlide4(
                    onNavigateToRegister = onNavigateToRegister,
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }

        // Tombol Next untuk Slide 1-3
        if (pagerState.currentPage < 3) {
            SavvyButton(
                text = if (pagerState.currentPage == 2) "Get Started!" else "Next",
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .height(50.dp),
                textColor = Navy,
                backgroundColor = Beige
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OnboardingSlide4(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // Logo dan slogan di tengah layar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Savvy (kecil, tengah)
            Image(
                painter = painterResource(id = R.drawable.logo_savvy_onboarding_4),
                contentDescription = "Savvy Logo",
                modifier = Modifier
                    .size(150.dp, 60.78.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tombol dan teks Log in di bagian bawah
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tombol Create Account
            SavvyButton(
                text = "Create Account",
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(50.dp),
                textColor = Navy,
                backgroundColor = Beige
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Teks "Have an account? Log in" dengan hanya "Log in" yang dapat diklik
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
                        onNavigateToLogin()
                    }
                },
                modifier = Modifier.padding(8.dp),
                style = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}