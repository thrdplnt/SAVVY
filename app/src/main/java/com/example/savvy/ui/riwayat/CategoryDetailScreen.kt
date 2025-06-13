package com.example.savvy.ui.riwayat

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    navController: NavController,
    category: String
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    val currentMonth = remember { Calendar.getInstance() }

    val transactions = remember { mutableStateListOf<Map<String, Any>>() }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserId = auth.currentUser?.uid ?: ""

    if (currentUserId.isEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        isLoading = true
        transactions.clear()

        val calendarStart = currentMonth.clone() as Calendar
        calendarStart.set(Calendar.DAY_OF_MONTH, 1)
        calendarStart.set(Calendar.HOUR_OF_DAY, 0)
        calendarStart.set(Calendar.MINUTE, 0)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)
        val startDate = calendarStart.time

        val calendarEnd = currentMonth.clone() as Calendar
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // PERBAIKAN: Menggunakan font dari theme
                    Text(
                        text = "Transaksi $category",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Navy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Navy)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White // Menggunakan warna dari theme
                )
            )
        },
        containerColor = White // Menggunakan warna dari theme
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                // PERBAIKAN: Box agar CircularProgressIndicator benar-benar di tengah layar
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Navy)
                }
            } else if (transactions.isEmpty()) {
                // PERBAIKAN: Menggunakan font dari theme dan memperbaiki format tanggal
                val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id"))
                Text(
                    text = "Tidak ada transaksi untuk kategori ini di bulan ${monthYearFormat.format(currentMonth.time)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Sedikit menambah jarak antar item
                ) {
                    items(transactions) { transaction ->
                        val amount = transaction["amount"] as? Long ?: 0L
                        // PERBAIKAN: Memperbaiki format tanggal
                        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id"))
                        val date = (transaction["date"] as? com.google.firebase.Timestamp)?.toDate()?.let {
                            dateFormat.format(it)
                        } ?: "Tanggal Tidak Diketahui"
                        val note = transaction["note"] as? String ?: "Tidak ada catatan"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), // Menyamakan corner radius dengan HomeScreen
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE6F0FA)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp) // Memberi jarak antar teks
                            ) {
                                // PERBAIKAN: Menggunakan font dari theme
                                Text(
                                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(amount)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Navy
                                )
                                // PERBAIKAN: Menggunakan font dari theme
                                Text(
                                    text = "Tanggal: $date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                // PERBAIKAN: Menggunakan font dari theme
                                Text(
                                    text = "Catatan: $note",
                                    style = MaterialTheme.typography.bodyMedium,
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