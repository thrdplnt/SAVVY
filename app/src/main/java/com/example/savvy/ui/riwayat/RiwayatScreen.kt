package com.example.savvy.ui.riwayat

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    // Hitung saldo dan filter transaksi
    fun filterTransactions() {
        Log.d("RiwayatScreen", "Filtering transactions. Wallet: $selectedWallet, Range: $selectedRange")
        val startTime = System.currentTimeMillis()
        var pemasukan = 0L
        var pengeluaran = 0L
        var tunai = 0L
        var tabungan = 0L
        var nonTunai = 0L

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
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
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
            val matchesDate = !transactionDate.before(startDateFilter) && !transactionDate.after(endDateFilter)
            if (matchesWallet && matchesDate) {
                filtered.add(transaction)
                if (transaction.category == "Pemasukan") {
                    pemasukan += transaction.amount
                } else {
                    pengeluaran += transaction.amount
                }
            }

            // Hitung saldo per dompet
            val amount = transaction.amount
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
        Log.d("RiwayatScreen", "Filtered ${filteredTransactions.size} transactions in ${System.currentTimeMillis() - startTime}ms")
    }

    LaunchedEffect(selectedWallet, selectedRange, selectedMonth.timeInMillis, startDate.timeInMillis, endDate.timeInMillis, transactions) {
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

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(24.dp),
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

            // Mutasi (Riwayat Transaksi)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Memastikan Card memanjang mengisi ruang yang tersedia
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
                ) {
                    Text(
                        text = "Riwayat",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Navy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    if (filteredTransactions.isEmpty()) {
                        Text(
                            text = "Tidak ada transaksi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Shadow,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Memastikan LazyColumn memanjang
                        ) {
                            items(filteredTransactions) { transaction ->
                                val isLocal = transaction.id.startsWith("local_")
                                TransactionItem(
                                    transaction = transaction,
                                    isLocal = isLocal,
                                    onClick = {
                                        Log.d("RiwayatScreen", "Navigating to DetailTransaksi with ID: ${transaction.id}")
                                        navController.navigate(Screen.DetailTransaksi.createRoute(transaction.id))
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}