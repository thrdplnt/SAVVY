package com.example.savvy.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.savvy.R // R akan digunakan untuk illustrationResId
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image // Import Image untuk Logo di TopAppBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.res.painterResource // Import untuk painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.example.savvy.R // Import R
import com.example.savvy.ui.components.PageIndicator
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.theme.*
import kotlinx.coroutines.launch

// Data class OnboardingSlideData tetap sama
data class OnboardingSlideData(
    val illustrationResId: Int,
    val title: String,
    val description: String
)

@Composable
fun OnboardingSlide(
    slideData: OnboardingSlideData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(White) // Menggunakan White dari theme Anda
    ) {
        // Logo Savvy DIHAPUS dari sini, akan dipindah ke TopAppBar OnboardingScreen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sesuaikan Spacer ini jika perlu setelah logo di TopAppBar ditambahkan
            Spacer(modifier = Modifier.height(60.dp)) // Memberi ruang jika TopAppBar memakan tempat

            Image(
                painter = painterResource(id = slideData.illustrationResId),
                contentDescription = "${slideData.title} Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.85f) // Bisa sedikit lebih besar
                    .aspectRatio(1f)
                    .padding(bottom = 24.dp)
            )
            Text(
                text = slideData.title,
                style = MaterialTheme.typography.headlineSmall.copy(color = Navy, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = slideData.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = Navy.copy(alpha = 0.8f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit,
    initialPage: Int = 0
) {
    val informationalPageCount = 3
    val totalPageCount = informationalPageCount + 1
    val pagerState = rememberPagerState(pageCount = { totalPageCount }, initialPage = initialPage)
    val coroutineScope = rememberCoroutineScope()

    val slides = listOf(
        OnboardingSlideData(R.drawable.onboarding_1, "Atur Keuanganmu dengan Savvy!", "Savvy membantu kamu mengelola keuangan pribadi dengan mudah. Mulai dari pencatatan hingga analisis, semua ada di sini!"),
        OnboardingSlideData(R.drawable.onboarding_2, "Catat dan Kelompokkan Pengeluaranmu!", "Pecahkan keuanganmu dengan fitur multi-dompet dan catat transaksi dengan mudah, manual atau melalui foto struk."),
        OnboardingSlideData(R.drawable.onboarding_3, "Pantau dan Rencanakan Keuanganmu!", "Lihat tren keuanganmu dengan grafik pie dan line, atur anggaran per kategori, dan ekspor laporan ke PDF kapan saja.")
    )

    Scaffold(
        topBar = {
            TopAppBar( // Menggunakan TopAppBar standar
                title = { /* Biarkan kosong agar ikon Previous & Logo lebih menonjol */ },
                navigationIcon = {
                    if (pagerState.currentPage > 0) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Previous", tint = Navy)
                        }
                    } else {
                        Spacer(Modifier.size(48.dp)) // Placeholder agar logo tetap di kanan jika tombol back tidak ada
                    }
                },
                actions = {
                    // Logo Savvy di kanan atas (sejajar tombol back)
                    Image(
                        painter = painterResource(id = R.drawable.logo_savvy_small),
                        contentDescription = "Savvy Logo",
                        modifier = Modifier
                            .padding(end = 16.dp) // Padding agar tidak terlalu mepet
                            .size(100.dp, 33.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White, // Latar belakang TopAppBar
                    navigationIconContentColor = Navy,
                    actionIconContentColor = Navy // Jika ada ikon di actions
                )
            )
        },
        bottomBar = {
            if (pagerState.currentPage < informationalPageCount) { // Hanya untuk slide 0, 1, 2
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, bottom = 40.dp, top = 20.dp), // Padding top ditambah agar lebih ke bawah
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PageIndicator(
                        pageCount = informationalPageCount,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.padding(bottom = 28.dp) // Jarak lebih besar ke tombol
                    )

                    SavvyButton(
                        text = if (pagerState.currentPage == informationalPageCount - 1) "Start!" else "Next",
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp)) // Jarak antara tombol Next dan Skip dikurangi

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(totalPageCount - 1)
                            }
                        }
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelMedium, color = Navy.copy(alpha = 0.7f)) // Style lebih kecil
                    }
                }
            } else {
                // Untuk slide terakhir (OnboardingSlide4), bottom bar ini tidak perlu
                // Spacer untuk memberi ruang jika OnboardingSlide4 tidak punya padding bawah yang cukup
                Spacer(modifier = Modifier.height(48.dp)) // Ketinggian yang cukup untuk estetika
            }
        },
        containerColor = White
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
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
    }
}


// Composable OnboardingSlide4
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
        // Konten Logo di tengah (yang Anda inginkan)
        Column(
            modifier = Modifier
                .fillMaxSize() // Mengisi seluruh ruang Box
                .padding(bottom = 200.dp), // Beri ruang di bawah untuk tombol-tombol
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Pusatkan logo di sisa ruang ini
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_savvy_onboarding_4),
                contentDescription = "Savvy Logo",
                modifier = Modifier.size(150.dp, 60.78.dp)
            )
        }

        // Tombol dan teks di bagian bawah
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Sejajarkan grup ini ke bawah Box
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp), // Padding dari tepi bawah layar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SavvyButton(
                text = "Create Account",
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                textColor = Navy,
                backgroundColor = Beige
            )
            Spacer(modifier = Modifier.height(16.dp))
            ClickableText(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Navy)) { append("Have an account? ") }
                    withStyle(style = SpanStyle(color = Navy, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) { append("Log in") }
                },
                onClick = { offset ->
                    val loginText = "Log in"
                    val loginStartIndex = "Have an account? ".length
                    if (offset in loginStartIndex until (loginStartIndex + loginText.length)) {
                        onNavigateToLogin()
                    }
                },
                modifier = Modifier.padding(8.dp),
                style = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Center)
            )
        }
    }
}