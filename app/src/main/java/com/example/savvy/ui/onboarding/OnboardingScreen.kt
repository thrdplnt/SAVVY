package com.example.savvy.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText // Impor ClickableText
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
import com.example.savvy.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // Warna latar belakang putih
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
                0 -> OnboardingSlide1()
                1 -> OnboardingSlide2()
                2 -> OnboardingSlide3()
                3 -> OnboardingSlide4(
                    onNavigateToRegister = onNavigateToRegister,
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }

        // Tombol Next untuk Slide 1-3
        if (pagerState.currentPage < 3) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Beige, // Warna tombol Beige
                    contentColor = Navy
                )
            ) {
                Text(
                    text = if (pagerState.currentPage == 2) "GET STARTED!" else "NEXT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OnboardingSlide1() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // Warna latar belakang putih
    ) {
        // Logo Savvy (posisi x = 42 dp, y = 62 dp dari kiri atas)
        Image(
            painter = painterResource(id = R.drawable.logo_savvy_small), // Logo kecil
            contentDescription = "Savvy Logo",
            modifier = Modifier
                .absoluteOffset(x = 42.dp, y = 42.dp) // Posisi absolut: x = 42 dp, y = 62 dp
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
            Spacer(modifier = Modifier.height(225.dp)) // Gap antara logo dan ilustrasi

            // Ilustrasi (di tengah)
            Image(
                painter = painterResource(id = R.drawable.onboarding_1), // Ganti dengan ilustrasi Slide 1
                contentDescription = "Onboarding 1 Illustration",
                modifier = Modifier
                    .size(300.dp) // Ukuran ilustrasi disesuaikan
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Judul
            Text(
                text = "Atur Keuanganmu dengan Savvy!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Navy // Menggunakan warna Navy dari ui.theme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deskripsi
            Text(
                text = "Savvy membantu kamu mengelola keuangan pribadi dengan mudah. Mulai dari pencatatan hingga analisis, semua ada di sini!",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Navy
            )
        }
    }
}

@Composable
fun OnboardingSlide2() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // Warna latar belakang putih
    ) {
        // Logo Savvy (posisi x = 42 dp, y = 62 dp dari kiri atas)
        Image(
            painter = painterResource(id = R.drawable.logo_savvy_small), // Logo kecil
            contentDescription = "Savvy Logo",
            modifier = Modifier
                .absoluteOffset(x = 42.dp, y = 42.dp) // Posisi absolut: x = 42 dp, y = 62 dp
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
                painter = painterResource(id = R.drawable.onboarding_2), // Ganti dengan ilustrasi Slide 2
                contentDescription = "Onboarding 2 Illustration",
                modifier = Modifier
                    .size(300.dp) // Ukuran ilustrasi disesuaikan
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Judul
            Text(
                text = "Catat dan Kelompokkan Pengeluaranmu!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Navy // Menggunakan warna Navy dari ui.theme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deskripsi
            Text(
                text = "Pecahkan keuanganmu dengan fitur multi-dompet dan catat transaksi dengan mudah, manual atau melalui foto struk.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Navy
            )
        }
    }
}

@Composable
fun OnboardingSlide3() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)) // Warna latar belakang putih
    ) {
        // Logo Savvy (posisi x = 42 dp, y = 62 dp dari kiri atas)
        Image(
            painter = painterResource(id = R.drawable.logo_savvy_small), // Logo kecil
            contentDescription = "Savvy Logo",
            modifier = Modifier
                .absoluteOffset(x = 42.dp, y = 42.dp) // Posisi absolut: x = 42 dp, y = 62 dp
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
                painter = painterResource(id = R.drawable.onboarding_3), // Ganti dengan ilustrasi Slide 3
                contentDescription = "Onboarding 3 Illustration",
                modifier = Modifier
                    .size(300.dp) // Ukuran ilustrasi disesuaikan
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Judul
            Text(
                text = "Pantau dan Rencanakan Keuanganmu!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Navy // Menggunakan warna Navy dari ui.theme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deskripsi
            Text(
                text = "Lihat tren keuanganmu dengan grafik pie dan line, atur anggaran per kategori, dan ekspor laporan ke PDF kapan saja.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Navy
            )
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
            .background(White) // Warna latar belakang putih
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
                painter = painterResource(id = R.drawable.logo_savvy_onboarding_4), // Logo kecil
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
            Button(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Beige, // Warna tombol Beige
                    contentColor = Navy
                )
            ) {
                Text(
                    text = "Create account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

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
                            textDecoration = TextDecoration.Underline // Garis bawah untuk menunjukkan dapat diklik
                        )
                    ) {
                        append("Log in")
                    }
                },
                onClick = { offset: Int ->
                    // Hanya bagian "Log in" yang akan memicu onNavigateToLogin
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