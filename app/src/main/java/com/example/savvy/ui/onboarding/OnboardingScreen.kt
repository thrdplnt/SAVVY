package com.example.savvy.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.savvy.R
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
            .background(White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp)) // Memberi ruang untuk TopAppBar di atas

            Image(
                painter = painterResource(id = slideData.illustrationResId),
                contentDescription = "${slideData.title} Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
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
            // PERBAIKAN: TopAppBar hanya akan tampil di 3 halaman pertama.
            if (pagerState.currentPage < informationalPageCount) {
                TopAppBar(
                    title = { /* Biarkan kosong */ },
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
                            Spacer(Modifier.size(48.dp)) // Placeholder agar logo tetap sejajar
                        }
                    },
                    actions = {
                        Image(
                            painter = painterResource(id = R.drawable.logo_savvy_small),
                            contentDescription = "Savvy Logo",
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(100.dp, 33.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = White
                    )
                )
            }
        },
        bottomBar = {
            if (pagerState.currentPage < informationalPageCount) { // Bottom bar untuk 3 slide pertama
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, bottom = 40.dp, top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PageIndicator(
                        pageCount = informationalPageCount,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.padding(bottom = 28.dp)
                    )

                    SavvyButton(
                        text = if (pagerState.currentPage == informationalPageCount - 1) "Mulai!" else "Lanjut", // Mengganti Start! & Next
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tombol Skip hanya tampil jika bukan di halaman terakhir (sebelum halaman 4)
                    if (pagerState.currentPage < informationalPageCount - 1) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(totalPageCount - 1)
                                }
                            }
                        ) {
                            Text("Lewati", style = MaterialTheme.typography.labelMedium, color = Navy.copy(alpha = 0.7f))
                        }
                    } else {
                        // Beri spacer agar tombol "Mulai!" tetap di posisi yang sama
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        },
        containerColor = White
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            userScrollEnabled = pagerState.currentPage < informationalPageCount // Scroll manual hanya di 3 slide pertama
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_savvy_onboarding_4),
                contentDescription = "Savvy Logo",
                modifier = Modifier.size(150.dp, 60.78.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SavvyButton(
                text = "Buat Akun",
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                textColor = Navy,
                backgroundColor = Beige
            )
            Spacer(modifier = Modifier.height(16.dp))
            ClickableText(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Navy)) { append("Sudah punya akun? ") }
                    withStyle(style = SpanStyle(color = Navy, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) { append("Masuk") }
                },
                onClick = { offset ->
                    // Logika klik tetap sama, hanya teks yang berubah
                    val loginText = "Masuk"
                    val loginStartIndex = "Sudah punya akun? ".length
                    if (offset in loginStartIndex until (loginStartIndex + loginText.length)) {
                        onNavigateToLogin()
                    }
                },
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
            )
        }
    }
}