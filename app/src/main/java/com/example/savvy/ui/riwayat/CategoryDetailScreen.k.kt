package com.example.savvy.ui.riwayat

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

    LaunchedEffect(Unit) {
        db.collection("transactions")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { querySnapshot ->
                transactions.clear()
                for (document in querySnapshot.documents) {
                    transactions.add(document.data ?: emptyMap())
                }
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

        LazyColumn {
            items(transactions) { transaction ->
                val amount = transaction["amount"] as? Long ?: 0L
                val date = (transaction["date"] as? com.google.firebase.Timestamp)?.toDate()?.toString() ?: "Unknown Date"
                val note = transaction["note"] as? String ?: "No note"

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