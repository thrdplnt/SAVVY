package com.example.savvy.ui.riwayat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savvy.ui.navigation.Screen
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.transaction.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RiwayatScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate(Screen.Login.route)
            return@LaunchedEffect
        }

        db.collection("transactions")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("RiwayatScreen", "Listen failed.", e)
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val transactionList = snapshot.documents.mapNotNull { doc ->
                        val transaction = doc.toObject(Transaction::class.java)
                        transaction?.copy(id = doc.id)
                    }
                    transactions = transactionList.sortedByDescending { it.date }
                    isLoading = false
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Menghapus BudgetSummary untuk sementara
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Riwayat Transaksi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Navy)
                }
            } else if (transactions.isEmpty()) {
                Text(
                    text = "Belum ada transaksi",
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
                        TransactionItem(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${transaction.category} (${transaction.type})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Navy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.date?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale("id")).format(it)
                    } ?: "Tanggal tidak tersedia",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (transaction.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Catatan: ${transaction.note}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(transaction.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
        }
    }
}