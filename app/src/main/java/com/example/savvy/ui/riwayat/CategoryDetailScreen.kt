package com.example.savvy.ui.riwayat

import androidx.compose.ui.Alignment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savvy.ui.theme.Navy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

@Composable
fun CategoryDetailScreen(
    navController: NavController,
    category: String
) {
    val db = FirebaseFirestore.getInstance()
    val transactions = remember { mutableStateListOf<Map<String, Any>>() }
    var isLoading by remember { mutableStateOf(true) }

    // Ambil userId dari Firebase Authentication
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Jika user belum login, arahkan ke halaman login
    if (currentUserId.isEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        Log.d("CategoryDetailScreen", "Fetching transactions for userId: $currentUserId, category: $category")
        db.collection("transactions")
            .whereEqualTo("category", category)
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                transactions.clear()
                Log.d("CategoryDetailScreen", "Found ${querySnapshot.documents.size} transactions")
                for (document in querySnapshot.documents) {
                    transactions.add(document.data ?: emptyMap())
                }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                Log.e("CategoryDetailScreen", "Error fetching transactions: $exception")
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Transaksi $category",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = Navy,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else if (transactions.isEmpty()) {
            Text(
                text = "Tidak ada transaksi untuk kategori ini",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            LazyColumn {
                items(transactions) { transaction ->
                    val amount = transaction["amount"] as? Long ?: 0L
                    val date = (transaction["date"] as? com.google.firebase.Timestamp)?.toDate()?.let {
                        java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id")).format(it)
                    } ?: "Tanggal Tidak Diketahui"
                    val note = transaction["note"] as? String ?: "Tidak ada catatan"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(amount)}",
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