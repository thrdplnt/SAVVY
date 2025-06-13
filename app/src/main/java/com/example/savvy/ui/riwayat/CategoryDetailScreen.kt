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

        db.collection("transactions")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("category", category)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
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

    // PERBAIKAN: Menghapus Scaffold dan menggunakan Column sebagai root layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // PERBAIKAN: Header statis tanpa TopAppBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp) // Padding untuk IconButton
                .padding(top = 24.dp, bottom = 16.dp), // 'Gap' yang konsisten
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Navy)
            }
            Text(
                text = "Transaksi $category",
                // PERBAIKAN: Style disamakan dengan 'Total Saldo' di halaman Riwayat
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Navy
            )
        }

        // PERBAIKAN: Konten utama yang bisa di-scroll
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Navy)
            }
        } else if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id"))
                Text(
                    text = "Tidak ada transaksi untuk kategori ini di bulan ${monthYearFormat.format(currentMonth.time)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { transaction ->
                    val amount = transaction["amount"] as? Long ?: 0L
                    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id"))
                    val date = (transaction["date"] as? com.google.firebase.Timestamp)?.toDate()?.let {
                        dateFormat.format(it)
                    } ?: "Tanggal Tidak Diketahui"
                    val note = transaction["note"] as? String ?: "Tidak ada catatan"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE6F0FA)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(amount)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Navy
                            )
                            Text(
                                text = "Tanggal: $date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
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