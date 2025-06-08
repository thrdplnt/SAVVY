package com.example.savvy.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.savvy.R
import com.example.savvy.data.Transaction
import com.example.savvy.ui.components.AnalysisItem
import com.example.savvy.ui.components.TransactionItem
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import java.text.NumberFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDompetDialog by remember { mutableStateOf(false) }
    var isSaldoVisible by rememberSaveable { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedPercentage by remember { mutableStateOf<Float?>(null) }

    // Warna untuk pie chart
    val categoryColors = mapOf(
        "Makanan" to Color(0xFF6256D1),
        "Transportasi" to Color(0xFF83E46F),
        "Hiburan" to Color(0xFF4894FF),
        "Pendidikan" to Color(0xFFFFD300),
        "Tagihan" to Color(0xFFFF4A4A),
        "Kesehatan" to Color(0xFF9DCFFF),
        "Belanja" to Color(0xFFFF458A),
        "Uang Keluar" to Color(0xFF76E7E7)
    )

    // Menangani tombol back fisik saat dalam mode pencarian
    BackHandler(enabled = uiState.isSearching) {
        viewModel.clearSearch()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_savvy_small),
                contentDescription = "Savvy Logo",
                modifier = Modifier.size(120.dp, 40.dp)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Cari kategori, catatan, atau tanggal", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = Navy) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, "Clear Search", tint = Navy)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Navy,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.performSearch()
                keyboardController?.hide()
            }),
            singleLine = true
        )

        // Tampilan kondisional
        if (uiState.isSearching) {
            SearchResultsContent(
                searchResults = uiState.searchResults,
                onTransactionClick = { transactionId ->
                    navController.navigate("detail_transaksi/$transactionId")
                }
            )
        } else {
            DashboardContent(
                uiState = uiState,
                isSaldoVisible = isSaldoVisible,
                onSaldoVisibilityToggle = { isSaldoVisible = !isSaldoVisible },
                onLihatSemuaDompetClick = { showDompetDialog = true },
                categoryColors = categoryColors,
                selectedCategory = selectedCategory,
                selectedPercentage = selectedPercentage,
                onCategorySelected = { category, percentage ->
                    selectedCategory = category
                    selectedPercentage = percentage
                },
                navController = navController
            )
        }
    }

    // Dialog untuk menampilkan semua dompet
    if (showDompetDialog) {
        AlertDialog(
            onDismissRequest = { showDompetDialog = false },
            title = { Text("Semua Dompet", color = Navy, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(uiState.walletsWithBalance) { walletItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(walletItem.wallet.name, color = Navy, fontSize = 16.sp)
                            Text(
                                text = if (isSaldoVisible) "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(walletItem.balance)}" else "****",
                                color = Navy,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDompetDialog = false }) {
                    Text("Tutup", color = Navy)
                }
            },
            containerColor = White
        )
    }
}

@Composable
fun DashboardContent(
    uiState: HomeUiState,
    isSaldoVisible: Boolean,
    onSaldoVisibilityToggle: () -> Unit,
    onLihatSemuaDompetClick: () -> Unit,
    categoryColors: Map<String, Color>,
    selectedCategory: String?,
    selectedPercentage: Float?,
    onCategorySelected: (String?, Float?) -> Unit,
    navController: NavController
) {
    Column {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Navy)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = if (isSaldoVisible) "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(uiState.totalSaldo)}" else "****",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isSaldoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Saldo Visibility",
                    tint = Navy,
                    modifier = Modifier
                        .clickable(onClick = onSaldoVisibilityToggle)
                        .size(24.dp)
                )
            }
            Text(
                text = "Total saldo",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )

            // Card Dompetku
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F0FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dompetku",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                        if (uiState.walletsWithBalance.size > 2) {
                            Text(
                                text = "Lihat selengkapnya",
                                fontSize = 14.sp,
                                color = Navy,
                                modifier = Modifier
                                    .clickable(onClick = onLihatSemuaDompetClick)
                                    .padding(start = 8.dp),
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                    Divider(
                        color = White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (uiState.walletsWithBalance.isEmpty()) {
                        Text(
                            text = "Belum ada dompet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Navy.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Tampilkan semua dompet di card, bukan hanya 2
                        uiState.walletsWithBalance.forEach { walletItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = walletItem.wallet.name,
                                    fontSize = 16.sp,
                                    color = Navy
                                )
                                Text(
                                    text = if (isSaldoVisible) "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(walletItem.balance)}" else "****",
                                    fontSize = 16.sp,
                                    color = Navy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Pie Chart
            if (uiState.totalPengeluaranBulanIni > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(150.dp)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press) {
                                                val offset = event.changes.first().position
                                                val centerX = size.width / 2f
                                                val centerY = size.height / 2f
                                                val dx = offset.x - centerX
                                                val dy = offset.y - centerY

                                                val distance = sqrt(dx * dx + dy * dy)
                                                val radius = size.width / 2f
                                                val innerRadius = radius - 40f

                                                if (distance in innerRadius..radius) {
                                                    var angle = atan2(dy, dx) * 180 / Math.PI
                                                    if (angle < 0) angle += 360

                                                    var startAngle = 0f
                                                    var selected: String? = null
                                                    var percentage: Float? = null

                                                    uiState.monthlyCategoryExpenses.forEach { (category, amount) ->
                                                        val sweepAngle = (amount.toFloat() / uiState.totalPengeluaranBulanIni.toFloat()) * 360f
                                                        if (angle >= startAngle && angle < startAngle + sweepAngle) {
                                                            selected = category
                                                            percentage = (amount.toFloat() / uiState.totalPengeluaranBulanIni.toFloat()) * 100f
                                                        }
                                                        startAngle += sweepAngle
                                                    }

                                                    onCategorySelected(selected, percentage)
                                                } else {
                                                    onCategorySelected(null, null)
                                                }
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                        ) {
                            var startAngle = 0f
                            uiState.monthlyCategoryExpenses.forEach { (category, amount) ->
                                val sweepAngle = (amount.toFloat() / uiState.totalPengeluaranBulanIni.toFloat()) * 360f
                                val isSelected = category == selectedCategory
                                drawArc(
                                    color = categoryColors[category] ?: Color.Gray,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width, size.height),
                                    style = Stroke(width = if (isSelected) 100f else 80f)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(
                            modifier = Modifier.width(100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (selectedCategory != null && selectedPercentage != null) {
                                Text(
                                    text = selectedCategory!!,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${String.format("%.1f", selectedPercentage)}%",
                                    fontSize = 12.sp,
                                    color = Navy,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Ketuk untuk\nmelihat detail",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Card Analisis
            if (uiState.monthlyCategoryExpenses.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Analisis Pengeluaran Bulan Ini",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Divider(
                            color = Navy.copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        uiState.monthlyCategoryExpenses.entries.sortedByDescending { it.value }.forEach { (category, totalAmount) ->
                            AnalysisItem(
                                category = category,
                                transactionCount = uiState.monthlyCategoryCounts[category] ?: 0,
                                totalAmount = totalAmount,
                                totalPengeluaran = uiState.totalPengeluaranBulanIni,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsContent(
    searchResults: List<Transaction>,
    onTransactionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Hasil Pencarian (${searchResults.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (searchResults.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada transaksi yang cocok.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    searchResults.forEachIndexed { index, transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                        if (index < searchResults.lastIndex) {
                            Divider(
                                color = Color.Gray.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}