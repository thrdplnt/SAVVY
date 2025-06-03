package com.example.savvy.ui.riwayat

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savvy.ui.theme.Navy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    navController: NavController,
    category: String
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    // State untuk bulan dan tahun yang dipilih (default: bulan saat ini)
    //selectedMonth tidak perlu menjadi var jika tidak ada interaksi untuk mengubahnya
    val selectedMonth = remember { Calendar.getInstance() } // Tidak perlu mutableStateOf jika tidak diubah

    // State untuk daftar transaksi yang difilter
    val transactions = remember { mutableStateListOf<Map<String, Any>>() }
    var isLoading by remember { mutableStateOf(true) }

    // Ambil userId dari Firebase Authentication
    val currentUserId = auth.currentUser?.uid ?: ""

    // Jika user belum login, arahkan ke halaman login
    if (currentUserId.isEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
        return
    }

    // LaunchedEffect akan berjalan sekali karena selectedMonth.timeInMillis tidak akan berubah di sini
    LaunchedEffect(Unit) { // UBAH: Dependensi menjadi Unit agar hanya berjalan sekali
        isLoading = true
        transactions.clear()

        val calendarStart = selectedMonth.clone() as Calendar
        calendarStart.set(Calendar.DAY_OF_MONTH, 1)
        calendarStart.set(Calendar.HOUR_OF_DAY, 0)
        calendarStart.set(Calendar.MINUTE, 0)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)
        val startDate = calendarStart.time

        val calendarEnd = selectedMonth.clone() as Calendar
        calendarEnd.set(Calendar.DAY_OF_MONTH, calendarEnd.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
        calendarEnd.set(Calendar.MINUTE, 59)
        calendarEnd.set(Calendar.SECOND, 59)
        calendarEnd.set(Calendar.MILLISECOND, 999)
        val endDate = calendarEnd.time

        Log.d("CategoryDetailScreen", "Fetching transactions for userId: $currentUserId, category: $category, from: $startDate to: $endDate")

        db.collection("transactions")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("category", category)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("CategoryDetailScreen", "Found ${querySnapshot.documents.size} transactions for this month.")
                for (document in querySnapshot.documents) {
                    transactions.add(document.data ?: emptyMap())
                }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                Log.e("CategoryDetailScreen", "Error fetching transactions: $exception")
                isLoading = false
                Toast.makeText(context, "Gagal mengambil transaksi: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // HAPUS BLOCK showMonthPicker (AlertDialog dan logic terkait) KARENA TIDAK AKAN DIGUNAKAN
    // if (showMonthPicker) { ... } // HAPUS INI

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transaksi $category", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Navy)
                    }
                },
                // HAPUS actions block KARENA TIDAK AKAN ADA DROPDOWN
                /*
                actions = {
                    Row(
                        modifier = Modifier
                            .clickable { showMonthPicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("MMMM yyyy", Locale("id")).format(selectedMonth.time),
                            color = Navy,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Bulan", tint = Navy)
                    }
                },
                */
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Navy,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else if (transactions.isEmpty()) {
                Text(
                    text = "Tidak ada transaksi untuk kategori ini di bulan ${SimpleDateFormat("MMMM yyyy", Locale("id")).format(selectedMonth.time)}.", // Perbaiki format tahun
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { transaction ->
                        val amount = transaction["amount"] as? Long ?: 0L
                        // Perbaiki format tanggal agar tidak ada karakter LaTeX
                        val date = (transaction["date"] as? com.google.firebase.Timestamp)?.toDate()?.let {
                            java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id")).format(it) // Gunakan format standar tanpa karakter aneh
                        } ?: "Tanggal Tidak Diketahui"
                        val note = transaction["note"] as? String ?: "Tidak ada catatan"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE6F0FA)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy
                                )
                                Text(
                                    text = "Tanggal: $date",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Catatan: $note",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}