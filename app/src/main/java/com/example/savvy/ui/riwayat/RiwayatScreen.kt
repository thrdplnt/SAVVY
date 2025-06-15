
package com.example.savvy.ui.riwayat

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.util.Log
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.savvy.R
import com.example.savvy.data.Screen
import com.example.savvy.data.Transaction
import com.example.savvy.ui.components.SimpleBarChart
import com.example.savvy.ui.components.TransactionItem
import com.example.savvy.ui.theme.*
import com.example.savvy.utils.ExportUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    navController: NavController,
    viewModel: RiwayatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val walletsState by viewModel.wallets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    // State untuk transaksi
    var filteredTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }

    // State untuk filter dan saldo
    var totalSaldo by rememberSaveable { mutableLongStateOf(0L) }
    var totalPemasukan by rememberSaveable { mutableLongStateOf(0L) }
    var totalPengeluaran by rememberSaveable { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaldoVisible by rememberSaveable { mutableStateOf(true) }

    // State untuk filter Dompetku
    val dynamicWalletOptions = remember(walletsState) {
        listOf("Semua") + walletsState.map { it.name }
    }
    var selectedWallet by remember(dynamicWalletOptions) {
        mutableStateOf(dynamicWalletOptions.firstOrNull() ?: "Semua")
    }
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
    val coroutineScope = rememberCoroutineScope()

    // Fungsi untuk memformat tanggal untuk subtitle
    val subtitleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id"))
    val subtitleMonthFormat = SimpleDateFormat("MMMM yyyy", Locale("id"))

    // Membuat string dinamis untuk subtitle grafik
    val filterSubtitle = remember(selectedWallet, selectedRange, selectedMonth.timeInMillis, startDate.timeInMillis, endDate.timeInMillis) {
        val dompetInfo = "Dompet: $selectedWallet"
        val periodeInfo = when (selectedRange) {
            "Pilih Bulan" -> "Periode: ${subtitleMonthFormat.format(selectedMonth.time)}"
            "Pilih Tanggal" -> "Periode: ${subtitleDateFormat.format(startDate.time)} - ${subtitleDateFormat.format(endDate.time)}"
            "1 Hari Terakhir" -> {
                val todayCal = Calendar.getInstance()
                "Periode: ${subtitleDateFormat.format(todayCal.time)}"
            }
            "7 Hari Terakhir" -> {
                val endCal = Calendar.getInstance()
                val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                "Periode: ${subtitleDateFormat.format(startCal.time)} - ${subtitleDateFormat.format(endCal.time)}"
            }
            else -> "Periode: $selectedRange"
        }
        "$dompetInfo - $periodeInfo"
    }

    // Hitung saldo, filter transaksi
    fun calculateAndFilterData() {
        Log.d("RiwayatScreen", "Filter triggered. VM_TX_Size: ${transactions.size}, Wallet: $selectedWallet, Range: $selectedRange")
        val startTime = System.currentTimeMillis()
        var currentPemasukanFiltered = 0L
        var currentPengeluaranFiltered = 0L

        // Hitung Saldo Total dari SEMUA transaksi
        val saldoPerDompet = mutableMapOf<String, Long>()
        walletsState.forEach { wallet -> saldoPerDompet[wallet.name] = 0L }

        transactions.forEach { transaction ->
            val amount = transaction.amount
            val walletName = transaction.type
            val currentBalance = saldoPerDompet[walletName] ?: 0L
            saldoPerDompet[walletName] = if (transaction.category == "Pemasukan") currentBalance + amount else currentBalance - amount
        }
        totalSaldo = saldoPerDompet.values.sum()
        Log.d("RiwayatScreen", "Total Saldo Recalculated: $totalSaldo from ${saldoPerDompet.size} wallets")

        // Filter transaksi untuk tampilan list dan kalkulasi Pemasukan/Pengeluaran terfilter
        val today = Calendar.getInstance()
        val startDateFilterCal = Calendar.getInstance()
        val endDateFilterCal = Calendar.getInstance()

        endDateFilterCal.time = today.time
        endDateFilterCal.set(Calendar.HOUR_OF_DAY, 23)
        endDateFilterCal.set(Calendar.MINUTE, 59)
        endDateFilterCal.set(Calendar.SECOND, 59)
        endDateFilterCal.set(Calendar.MILLISECOND, 999)

        when (selectedRange) {
            "1 Hari Terakhir" -> {
                startDateFilterCal.time = today.time
                startDateFilterCal.set(Calendar.HOUR_OF_DAY, 0)
                startDateFilterCal.set(Calendar.MINUTE, 0)
                startDateFilterCal.set(Calendar.SECOND, 0)
                startDateFilterCal.set(Calendar.MILLISECOND, 0)
            }
            "7 Hari Terakhir" -> {
                startDateFilterCal.time = today.time
                startDateFilterCal.add(Calendar.DAY_OF_YEAR, -6)
                startDateFilterCal.set(Calendar.HOUR_OF_DAY, 0)
                startDateFilterCal.set(Calendar.MINUTE, 0)
                startDateFilterCal.set(Calendar.SECOND, 0)
                startDateFilterCal.set(Calendar.MILLISECOND, 0)
            }
            "Pilih Bulan" -> {
                startDateFilterCal.time = selectedMonth.time
                startDateFilterCal.set(Calendar.DAY_OF_MONTH, 1)
                startDateFilterCal.set(Calendar.HOUR_OF_DAY, 0)
                startDateFilterCal.set(Calendar.MINUTE, 0)
                startDateFilterCal.set(Calendar.SECOND, 0)
                startDateFilterCal.set(Calendar.MILLISECOND, 0)
                endDateFilterCal.time = selectedMonth.time
                endDateFilterCal.set(Calendar.DAY_OF_MONTH, endDateFilterCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endDateFilterCal.set(Calendar.HOUR_OF_DAY, 23)
                endDateFilterCal.set(Calendar.MINUTE, 59)
                endDateFilterCal.set(Calendar.SECOND, 59)
                endDateFilterCal.set(Calendar.MILLISECOND, 999)
            }
            "Pilih Tanggal" -> {
                startDateFilterCal.time = startDate.time
                startDateFilterCal.set(Calendar.HOUR_OF_DAY, 0)
                startDateFilterCal.set(Calendar.MINUTE, 0)
                startDateFilterCal.set(Calendar.SECOND, 0)
                startDateFilterCal.set(Calendar.MILLISECOND, 0)
                endDateFilterCal.time = endDate.time
                endDateFilterCal.set(Calendar.HOUR_OF_DAY, 23)
                endDateFilterCal.set(Calendar.MINUTE, 59)
                endDateFilterCal.set(Calendar.SECOND, 59)
                endDateFilterCal.set(Calendar.MILLISECOND, 999)
            }
            else -> {
                startDateFilterCal.add(Calendar.YEAR, -100)
            }
        }
        val startDateFilterDate = startDateFilterCal.time
        val endDateFilterDate = endDateFilterCal.time

        val finalFilteredList = transactions.filter { transaction ->
            val transactionDate = transaction.date ?: return@filter false
            val matchesWallet = selectedWallet == "Semua" || transaction.type == selectedWallet
            val matchesDate = !transactionDate.before(startDateFilterDate) && !transactionDate.after(endDateFilterDate)
            matchesWallet && matchesDate
        }

        finalFilteredList.forEach {
            if (it.category == "Pemasukan") currentPemasukanFiltered += it.amount
            else currentPengeluaranFiltered += it.amount
        }

        totalPemasukan = currentPemasukanFiltered
        totalPengeluaran = currentPengeluaranFiltered
        filteredTransactions = finalFilteredList.sortedByDescending { it.date }
        Log.d("RiwayatScreen", "Filter End. Pemasukan(filter): $totalPemasukan, Pengeluaran(filter): $totalPengeluaran. Filtered List: ${filteredTransactions.size}")
    }

    LaunchedEffect(selectedWallet, selectedRange, selectedMonth.timeInMillis, startDate.timeInMillis, endDate.timeInMillis, transactions) {
        isLoading = true
        try {
            calculateAndFilterData()
        } catch (e: Exception) {
            Log.e("RiwayatScreen", "Error in LaunchedEffect: $e", e)
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
            containerColor = Color.White
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
            containerColor = Color.White
        )
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
                                val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_savvy_small)
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
            containerColor = Color.White
        )
    }

    // UI Utama tanpa Scaffold
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 24.dp) // Hanya padding horizontal untuk konten
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Navy,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                // Total Saldo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSaldoVisible) "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(totalSaldo)}" else "Rp ••••••",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Navy
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { isSaldoVisible = !isSaldoVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaldoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Saldo Visibility",
                                tint = Navy.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Saldo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Shadow,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

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
                                .background(Color.White)
                                .fillMaxWidth()
                        ) {
                            dynamicWalletOptions.forEach { option ->
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
                                .background(Color.White)
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
                                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalPemasukan)}",
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
                                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalPengeluaran)}",
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
                                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalPemasukan - totalPengeluaran)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (totalPemasukan - totalPengeluaran >= 0) Navy else ErrorRed
                            )
                        }
                    }
                }

                // Grafik Batang
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Diagram Pemasukan dan Pengeluaran",
                            style = MaterialTheme.typography.titleMedium,
                            color = Navy,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = filterSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Navy.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (totalPemasukan > 0 || totalPengeluaran > 0) {
                            SimpleBarChart(pemasukan = totalPemasukan, pengeluaran = totalPengeluaran)
                        } else {
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
                        containerColor = Color.White
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
                        Divider(
                            color = Navy.copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
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

        // FloatingActionButton ditempatkan secara manual
        FloatingActionButton(
            onClick = { showExportDialog = true },
            containerColor = Navy,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = "Ekspor Riwayat")
        }
    }
}
