package com.example.savvy.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.savvy.data.Transaction
import com.example.savvy.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailTransaksiScreen(
    navController: NavController,
    transactionId: String
) {
    val db = FirebaseFirestore.getInstance()
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showImageDialog by remember { mutableStateOf(false) } // State untuk menampilkan dialog gambar

    // Ambil data transaksi berdasarkan transactionId
    LaunchedEffect(transactionId) {
        db.collection("transactions")
            .document(transactionId)
            .get()
            .addOnSuccessListener { document ->
                transaction = document.toObject(Transaction::class.java)?.copy(id = document.id)
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .padding(vertical = 24.dp) // Margin vertikal untuk seluruh layar
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Baris untuk tombol kembali dan judul
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp), // Margin horizontal untuk tombol dan judul
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Kembali
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Navy
                )
            }

            // Spacer untuk memberikan jarak antara tombol dan judul
            Spacer(modifier = Modifier.width(8.dp))

            // Judul
            Text(
                text = "Detail Transaksi",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                modifier = Modifier.weight(1f) // Mengisi sisa ruang
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Konten utama dengan box putih
        if (isLoading) {
            CircularProgressIndicator(
                color = Navy,
                modifier = Modifier.size(48.dp)
            )
        } else if (transaction != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp) // Margin horizontal sejajar dengan tombol "Back"
                    .background(White, shape = RoundedCornerShape(8.dp)) // Box putih dengan sudut membulat
                    .padding(16.dp), // Padding di dalam box putih
                horizontalAlignment = Alignment.Start
            ) {
                // Kategori
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction!!.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Jumlah
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
                        text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(transaction!!.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (transaction!!.category == "Pemasukan") Navy else ErrorRed
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Tipe (Dompet)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tipe",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction!!.type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Tanggal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tanggal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction!!.date?.let { dateFormat.format(it) } ?: "Tanggal Tidak Diketahui",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Catatan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Catatan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction!!.note.ifEmpty { "Tidak ada catatan" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Wallet ID (jika ada)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ID Dompet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction!!.walletId.ifEmpty { "Tidak ada ID Dompet" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Navy
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Gambar (jika ada)
                transaction!!.imageUrl?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bukti Transaksi",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy,
                        modifier = Modifier.align(Alignment.CenterHorizontally) // Tulisan "Bukti Transaksi" di tengah
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Bukti Transaksi",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f) // Rasio 3:4 untuk gambar
                            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                            .clickable { showImageDialog = true } // Gambar dapat diklik
                    )

                    // Dialog untuk menampilkan gambar dalam ukuran lebih besar
                    if (showImageDialog) {
                        AlertDialog(
                            onDismissRequest = { showImageDialog = false },
                            confirmButton = {
                                TextButton(
                                    onClick = { showImageDialog = false }
                                ) {
                                    Text(
                                        text = "Tutup",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Navy
                                    )
                                }
                            },
                            text = {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Bukti Transaksi (Penuh)",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f) // Tetap mempertahankan rasio 3:4
                                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                                )
                            },
                            containerColor = White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp) // Margin horizontal sejajar dengan tombol "Back"
                    .background(White, shape = RoundedCornerShape(8.dp)) // Box putih untuk pesan error
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Transaksi tidak ditemukan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ErrorRed
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}