package com.example.savvy.ui.riwayat

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.savvy.data.Screen
import com.example.savvy.data.Transaction
import com.example.savvy.ui.components.TransactionItem
import com.example.savvy.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt
import com.example.savvy.ui.components.SimpleBarChart
import android.graphics.BitmapFactory
import androidx.compose.material.icons.filled.Download
import com.example.savvy.R
import com.example.savvy.utils.ExportUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    navController: NavController,
    viewModel: RiwayatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // State untuk transaksi
    val transactions by viewModel.transactions.collectAsState()
    var filteredTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }

    // State untuk filter dan saldo
    var totalSaldo by rememberSaveable { mutableLongStateOf(0L) }
    var saldoTunai by rememberSaveable { mutableLongStateOf(0L) }
    var saldoTabungan by rememberSaveable { mutableLongStateOf(0L) }
    var saldoNonTunai by rememberSaveable { mutableLongStateOf(0L) }
    var totalPemasukan by rememberSaveable { mutableLongStateOf(0L) }
    var totalPengeluaran by rememberSaveable { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    // State untuk filter Dompetku
    val walletOptions = listOf("Semua", "Tunai", "Tabungan", "Non-Tunai")
    var selectedWallet by remember { mutableStateOf("Semua") }
    var walletExpanded by remember { mutableStateOf(false) }

    // State untuk filter Rentang
    val rangeOptions = listOf("1 Hari Terakhir", "7 Hari Terakhir", "Pilih Bulan", "Pilih Tanggal")
    var selectedRange by remember { mutableStateOf("Pilih Bulan") }
    var rangeExpanded by remember { mutableStateOf(false) }

    // State untuk pemilihan bulan
    var showMonthPicker by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }

    // State untuk pemilihan tanggal
    var showDateRangePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(Calendar.getInstance()) }
    var endDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope() // Untuk menjalankan operasi ekspor di coroutine

    // State untuk pie chart
    val categoryData = remember { mutableStateMapOf<String, Pair<Int, Long>>() }
    var totalChartAmount by rememberSaveable { mutableLongStateOf(0L) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedPercentage by remember { mutableStateOf<Float?>(null) }

    // Warna untuk pie chart
    val categoryColors = mapOf(
        "Pemasukan" to Color(0xFF4CAF50), // Hijau untuk pemasukan
        "Makanan" to Color(0xFF6256D1),
        "Transportasi" to Color(0xFF83E46F),
        "Hiburan" to Color(0xFF4894FF),
        "Pendidikan" to Color(0xFFFFD300),
        "Tagihan" to Color(0xFFFF4A4A),
        "Kesehatan" to Color(0xFF9DCFFF),
        "Belanja" to Color(0xFFFF458A),
        "Uang Keluar" to Color(0xFF76E7E7)
    )

    // Fungsi untuk memformat tanggal untuk subtitle
    val subtitleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id"))
    val subtitleMonthFormat = SimpleDateFormat("MMMM yyyy", Locale("id"))

    // Membuat string dinamis untuk subtitle grafik
    val filterSubtitle = remember(
        selectedWallet,
        selectedRange,
        selectedMonth.timeInMillis, // Gunakan timeInMillis sebagai key
        startDate.timeInMillis,     // Gunakan timeInMillis sebagai key
        endDate.timeInMillis        // Gunakan timeInMillis sebagai key
    ) {
        val dompetInfo = "Dompet: $selectedWallet"
        val periodeInfo = when (selectedRange) {
            "Pilih Bulan" -> "Periode: ${subtitleMonthFormat.format(selectedMonth.time)}"
            "Pilih Tanggal" -> "Periode: ${subtitleDateFormat.format(startDate.time)} - ${subtitleDateFormat.format(endDate.time)}"
            "1 Hari Terakhir" -> {
                // Untuk "1 Hari Terakhir", kita tampilkan tanggal hari ini
                val todayCal = Calendar.getInstance()
                "Periode: ${subtitleDateFormat.format(todayCal.time)}"
            }
            "7 Hari Terakhir" -> {
                // Untuk "7 Hari Terakhir", kita tampilkan rentang 7 hari ke belakang dari hari ini
                val endCal = Calendar.getInstance()
                val startCal = Calendar.getInstance()
                startCal.add(Calendar.DAY_OF_YEAR, -6)
                "Periode: ${subtitleDateFormat.format(startCal.time)} - ${subtitleDateFormat.format(endCal.time)}"
            }
            else -> "Periode: $selectedRange" // Fallback jika ada range lain
        }
        val subtitle = "$dompetInfo - $periodeInfo"
        Log.d("RiwayatScreen", "Subtitle Dihitung Ulang: $subtitle") // Tambahkan log untuk debug
        subtitle
    }

    // Hitung saldo, filter transaksi, dan data pie chart
    fun filterTransactions() {
        Log.d(
            "RiwayatScreen",
            "Filtering transactions. Wallet: $selectedWallet, Range: $selectedRange"
        )
        val startTime = System.currentTimeMillis()
        var pemasukan = 0L
        var pengeluaran = 0L
        var tunai = 0L
        var tabungan = 0L
        var nonTunai = 0L
        val categoryMap = mutableMapOf<String, Pair<Int, Long>>()

        val calendar = Calendar.getInstance()
        val currentDate = calendar.time

        val startDateFilter: Date
        var endDateFilter: Date = currentDate

        when (selectedRange) {
            "1 Hari Terakhir" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateFilter = calendar.time
                calendar.time = currentDate
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDateFilter = calendar.time
            }

            "7 Hari Terakhir" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateFilter = calendar.time
                calendar.time = currentDate
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDateFilter = calendar.time
            }

            "Pilih Bulan" -> {
                calendar.time = selectedMonth.time
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateFilter = calendar.time
                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDateFilter = calendar.time
            }

            "Pilih Tanggal" -> {
                calendar.time = startDate.time
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateFilter = calendar.time
                calendar.time = endDate.time
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDateFilter = calendar.time
            }

            else -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateFilter = calendar.time
                calendar.time = currentDate
                endDateFilter = calendar.time
            }
        }

        val filtered = mutableListOf<Transaction>()
        transactions.forEach { transaction ->
            val transactionDate = transaction.date ?: return@forEach
            val matchesWallet = selectedWallet == "Semua" || transaction.type == selectedWallet
            val matchesDate =
                !transactionDate.before(startDateFilter) && !transactionDate.after(endDateFilter)
            if (matchesWallet && matchesDate) {
                filtered.add(transaction)
                val amount = transaction.amount.toLong()
                if (transaction.category == "Pemasukan") {
                    pemasukan += amount
                    // Tambahkan Pemasukan ke pie chart
                    val currentData = categoryMap["Pemasukan"] ?: Pair(0, 0L)
                    categoryMap["Pemasukan"] =
                        Pair(currentData.first + 1, currentData.second + amount)
                } else {
                    pengeluaran += amount
                    // Tambahkan kategori pengeluaran ke pie chart
                    val currentData = categoryMap[transaction.category] ?: Pair(0, 0L)
                    categoryMap[transaction.category] =
                        Pair(currentData.first + 1, currentData.second + amount)
                }
            }

            // Hitung saldo per dompet
            val amount = transaction.amount.toLong()
            when (transaction.type) {
                "Tunai" -> if (transaction.category == "Pemasukan") tunai += amount else tunai -= amount
                "Tabungan" -> if (transaction.category == "Pemasukan") tabungan += amount else tabungan -= amount
                "Non-Tunai" -> if (transaction.category == "Pemasukan") nonTunai += amount else nonTunai -= amount
            }
        }

        totalPemasukan = pemasukan
        totalPengeluaran = pengeluaran
        saldoTunai = tunai
        saldoTabungan = tabungan
        saldoNonTunai = nonTunai
        totalSaldo = tunai + tabungan + nonTunai
        filteredTransactions = filtered.sortedByDescending { it.date }
        categoryData.clear()
        categoryData.putAll(categoryMap)
        totalChartAmount = pemasukan + pengeluaran
        Log.d(
            "RiwayatScreen",
            "Filtered ${filteredTransactions.size} transactions in ${System.currentTimeMillis() - startTime}ms, " +
                    "Pie chart data: $categoryMap, Total: $totalChartAmount"
        )
    }

    LaunchedEffect(
        selectedWallet,
        selectedRange,
        selectedMonth.timeInMillis,
        startDate.timeInMillis,
        endDate.timeInMillis,
        transactions
    ) {
        isLoading = true
        try {
            filterTransactions()
        } catch (e: Exception) {
            Log.e("RiwayatScreen", "Error filtering transactions: $e")
        } finally {
            isLoading = false
        }
    }

    // Dialog untuk memilih tanggal mulai
    if (showStartDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate.set(Calendar.YEAR, year)
                startDate.set(Calendar.MONTH, month)
                startDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showStartDatePicker = false
                if (endDate.before(startDate)) {
                    endDate = startDate.clone() as Calendar
                }
            },
            startDate.get(Calendar.YEAR),
            startDate.get(Calendar.MONTH),
            startDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Dialog untuk memilih tanggal akhir
    if (showEndDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endDate.set(Calendar.YEAR, year)
                endDate.set(Calendar.MONTH, month)
                endDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showEndDatePicker = false
                if (endDate.before(startDate)) {
                    startDate = endDate.clone() as Calendar
                }
            },
            endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH),
            endDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Dialog untuk memilih bulan
    if (showMonthPicker) {
        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val years = (2020..2030).toList()
        var tempMonth by remember { mutableStateOf(selectedMonth.get(Calendar.MONTH)) }
        var tempYear by remember { mutableStateOf(selectedMonth.get(Calendar.YEAR)) }

        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = {
                Text(
                    text = "Pilih Bulan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Navy
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(months) { month ->
                            val monthIndex = months.indexOf(month)
                            Text(
                                text = month,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (tempMonth == monthIndex) Navy else Shadow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable {
                                        tempMonth = monthIndex
                                    }
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(years) { year ->
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (tempYear == year) Navy else Shadow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable {
                                        tempYear = year
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedMonth.set(Calendar.YEAR, tempYear)
                        selectedMonth.set(Calendar.MONTH, tempMonth)
                        showMonthPicker = false
                    }
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMonthPicker = false }
                ) {
                    Text(
                        text = "Batal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
            },
            containerColor = White
        )
    }

    // Dialog untuk memilih rentang tanggal
    if (showDateRangePicker) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        AlertDialog(
            onDismissRequest = { showDateRangePicker = false },
            title = {
                Text(
                    text = "Pilih Rentang Tanggal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Navy
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tanggal Mulai",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                        Text(
                            text = dateFormat.format(startDate.time),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Navy,
                            modifier = Modifier
                                .clickable { showStartDatePicker = true }
                                .padding(8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tanggal Akhir",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                        Text(
                            text = dateFormat.format(endDate.time),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Navy,
                            modifier = Modifier
                                .clickable { showEndDatePicker = true }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDateRangePicker = false }
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDateRangePicker = false }
                ) {
                    Text(
                        text = "Batal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
            },
            containerColor = White
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExportDialog = true },
                containerColor = Navy,
                contentColor = White
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Ekspor Riwayat")
            }
        },
        floatingActionButtonPosition = FabPosition.End // Posisi di kanan bawah
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) // Penting untuk Scaffold
                .fillMaxSize()
                .background(White)
                .padding(horizontal = 16.dp, vertical = 16.dp) // Padding untuk konten utama
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading && transactions.isEmpty()) {
                Spacer(modifier = Modifier.height(60.dp))
                CircularProgressIndicator(color = Navy, modifier = Modifier.size(48.dp))
            } else {
                Text("Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalSaldo)}", style = MaterialTheme.typography.headlineLarge, color = Navy, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) // Mengurangi padding atas
                Text("Total Saldo Saat Ini", style = MaterialTheme.typography.labelMedium, color = Shadow, modifier = Modifier.padding(bottom = 16.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) { /* Dropdown Dompet */
                        OutlinedButton(onClick = { walletExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)) {
                            Text(selectedWallet, style = MaterialTheme.typography.bodyLarge); Icon(Icons.Default.ArrowDropDown, "Dropdown", tint = Navy)
                        }
                        DropdownMenu(expanded = walletExpanded, onDismissRequest = { walletExpanded = false }, modifier = Modifier.background(White).fillMaxWidth()) {
                            walletOptions.forEach { option -> DropdownMenuItem(text = { Text(option, style = MaterialTheme.typography.bodyLarge) }, onClick = { selectedWallet = option; walletExpanded = false }) }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) { /* Dropdown Rentang */
                        OutlinedButton(onClick = { rangeExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)) {
                            Text(selectedRange, style = MaterialTheme.typography.bodyLarge); Icon(Icons.Default.ArrowDropDown, "Dropdown", tint = Navy)
                        }
                        DropdownMenu(expanded = rangeExpanded, onDismissRequest = { rangeExpanded = false }, modifier = Modifier.background(White).fillMaxWidth()) {
                            rangeOptions.forEach { option -> DropdownMenuItem(text = { Text(option, style = MaterialTheme.typography.bodyLarge) }, onClick = { selectedRange = option; rangeExpanded = false; if (option == "Pilih Bulan") showMonthPicker = true else if (option == "Pilih Tanggal") showDateRangePicker = true }) }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Beige), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)){
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pemasukan (Filter)", style = MaterialTheme.typography.bodyLarge, color = Navy); Text("Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalPemasukan)}", style = MaterialTheme.typography.bodyMedium, color = GreenSavvy)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pengeluaran (Filter)", style = MaterialTheme.typography.bodyLarge, color = Navy); Text("Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalPengeluaran)}", style = MaterialTheme.typography.bodyMedium, color = ErrorRed)
                        }
                        Spacer(modifier = Modifier.height(12.dp)); Divider(color = Navy.copy(alpha = 0.5f), thickness = 1.dp); Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Selisih (Filter)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Navy); Text("Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalPemasukan - totalPengeluaran)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (totalPemasukan - totalPengeluaran >= 0) GreenSavvy else ErrorRed)
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Grafik Pemasukan dan Pengeluaran", style = MaterialTheme.typography.titleMedium, color = Navy, modifier = Modifier.padding(bottom = 16.dp))
                        if ((totalPemasukan > 0 || totalPengeluaran > 0) && !isLoading) {
                            SimpleBarChart(pemasukan = totalPemasukan, pengeluaran = totalPengeluaran)
                        } else if (!isLoading) {
                            Text("Tidak ada data untuk grafik.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, color = Shadow, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                        Text("Riwayat Transaksi (Filter)", style = MaterialTheme.typography.titleLarge, color = Navy, modifier = Modifier.padding(bottom = 12.dp))
                        if (filteredTransactions.isEmpty() && !isLoading) {
                            Text("Tidak ada transaksi untuk filter ini.", style = MaterialTheme.typography.bodyMedium, color = Shadow, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp))
                        } else if (!isLoading) {
                            filteredTransactions.forEach { transaction ->
                                TransactionItem(transaction = transaction, onClick = { navController.navigate(Screen.DetailTransaksi.createRoute(transaction.id)) })
                                Divider(color = Shadow.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Dialog untuk pilihan ekspor
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Pilih Format Ekspor", color = Navy) },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_savvy_small) // Ganti dengan logo Anda
                                    val uri = ExportUtils.exportTransactionsToPdf(
                                        context,
                                        filteredTransactions,
                                        "Riwayat_Transaksi_Savvy_${System.currentTimeMillis()}",
                                        "Laporan Riwayat Transaksi Savvy",
                                        logoBitmap
                                    )
                                    uri?.let { ExportUtils.openFile(context, it, "application/pdf") }
                                }
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Ekspor ke PDF", color = Navy) }

                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val uri = ExportUtils.exportTransactionsToCsv(
                                        context,
                                        filteredTransactions,
                                        "Riwayat_Transaksi_Savvy_${System.currentTimeMillis()}"
                                    )
                                    uri?.let { ExportUtils.openFile(context, it, "text/csv") }
                                }
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Ekspor ke CSV", color = Navy) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) { Text("Batal", color = Navy) }
                },
                containerColor = White
            )
        }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Total Saldo
        if (isLoading) {
            CircularProgressIndicator(
                color = Navy,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Text(
                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalSaldo)}",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Total saldo",
                style = MaterialTheme.typography.labelSmall,
                color = Shadow,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Dropdown Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedButton(
                        onClick = { walletExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Navy
                        )
                    ) {
                        Text(
                            text = selectedWallet,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Navy
                        )
                    }
                    DropdownMenu(
                        expanded = walletExpanded,
                        onDismissRequest = { walletExpanded = false },
                        modifier = Modifier
                            .background(White)
                            .fillMaxWidth()
                    ) {
                        walletOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    selectedWallet = option
                                    walletExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedButton(
                        onClick = { rangeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Navy
                        )
                    ) {
                        Text(
                            text = selectedRange,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Navy
                        )
                    }
                    DropdownMenu(
                        expanded = rangeExpanded,
                        onDismissRequest = { rangeExpanded = false },
                        modifier = Modifier
                            .background(White)
                            .fillMaxWidth()
                    ) {
                        rangeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    selectedRange = option
                                    rangeExpanded = false
                                    if (option == "Pilih Bulan") {
                                        showMonthPicker = true
                                    } else if (option == "Pilih Tanggal") {
                                        showDateRangePicker = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Pemasukan dan Pengeluaran
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Beige
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pemasukan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                        Text(
                            text = "Rp ${
                                NumberFormat.getNumberInstance(Locale("id")).format(totalPemasukan)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Navy
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pengeluaran",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                        Text(
                            text = "Rp ${
                                NumberFormat.getNumberInstance(Locale("id"))
                                    .format(totalPengeluaran)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Navy
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(
                        color = Navy,
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Jumlah",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                        Text(
                            text = "Rp ${
                                NumberFormat.getNumberInstance(Locale("id"))
                                    .format(totalPemasukan - totalPengeluaran)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (totalPemasukan - totalPengeluaran >= 0) Navy else ErrorRed
                        )
                    }
                }
            }

            // GRAFIK BATANG BARU
//            if ((totalPemasukan > 0 || totalPengeluaran > 0) && !isLoading) { // Hanya tampilkan jika ada data & tidak loading
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Grafik Pemasukan dan Pengeluaran", // JUDUL UTAMA GRAFIK
                        style = MaterialTheme.typography.titleMedium, // Anda bisa sesuaikan stylenya
                        color = Navy,
                        modifier = Modifier.padding(bottom = 4.dp) // Jarak ke subjudul
                    )
                    Text(
                        text = filterSubtitle, // SUBJUDUL DINAMIS
                        style = MaterialTheme.typography.bodySmall, // Style untuk subjudul
                        color = Navy.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp) // Jarak ke grafik
                    )

                    if ((totalPemasukan > 0 || totalPengeluaran > 0) && !isLoading) {
                        SimpleBarChart(
                            pemasukan = totalPemasukan,
                            pengeluaran = totalPengeluaran
                        )
                    } else if (!isLoading) {
                        Text(
                            "Tidak ada data untuk grafik pada filter ini.",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = Shadow,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

                // Mutasi (Riwayat Transaksi)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Riwayat",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Navy,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (filteredTransactions.isEmpty()) {
                            Text(
                                text = "Tidak ada transaksi",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Shadow,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            filteredTransactions.forEach { transaction ->
                                val isLocal = transaction.id.startsWith("local_")
                                TransactionItem(
                                    transaction = transaction,
                                    isLocal = isLocal,
                                    onClick = {
                                        Log.d(
                                            "RiwayatScreen",
                                            "Navigating to DetailTransaksi with ID: ${transaction.id}"
                                        )
                                        navController.navigate(
                                            Screen.DetailTransaksi.createRoute(
                                                transaction.id
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
