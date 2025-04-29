package com.example.savvy.ui.riwayat

import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.savvy.data.Screen
import com.example.savvy.data.Transaction
import com.example.savvy.ui.theme.*
import com.example.savvy.ui.tambah.TambahTransaksiViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailTransaksiScreen(
    navController: NavController,
    transactionId: String,
    viewModel: TambahTransaksiViewModel = hiltViewModel()
) {
    val db = FirebaseFirestore.getInstance()
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Log.d("DetailTransaksiScreen", "Transaction ID: $transactionId")

    LaunchedEffect(transactionId) {
        isLoading = true
        try {
            if (transactionId.startsWith("local_")) {
                val localId = transactionId.removePrefix("local_").toLongOrNull()
                if (localId != null) {
                    val localTransaction = viewModel.localTransactionDao.getTransactionByLocalId(localId)
                    if (localTransaction != null) {
                        transaction = Transaction(
                            id = transactionId,
                            userId = localTransaction.userId,
                            walletId = localTransaction.walletId ?: "",
                            type = localTransaction.type,
                            amount = localTransaction.amount,
                            category = localTransaction.category,
                            note = localTransaction.note,
                            date = localTransaction.date,
                            imageUrl = localTransaction.imageUrl,
                            imageUri = localTransaction.imageUri
                        )
                        Log.d("DetailTransaksiScreen", "Loaded local transaction: $localTransaction, imageUri: ${localTransaction.imageUri}")
                    } else {
                        Log.w("DetailTransaksiScreen", "Local transaction not found for ID: $localId")
                        transaction = null
                    }
                } else {
                    Log.w("DetailTransaksiScreen", "Invalid local transaction ID: $transactionId")
                    transaction = null
                }
            } else {
                val document = db.collection("transactions")
                    .document(transactionId)
                    .get()
                    .await()
                transaction = document.toObject(Transaction::class.java)?.copy(id = document.id)
                if (transaction != null) {
                    // Cari transaksi lokal untuk mendapatkan imageUri
                    val localTransaction = viewModel.localTransactionDao.getByFirestoreId(transactionId)
                    if (localTransaction != null) {
                        transaction = transaction!!.copy(imageUri = localTransaction.imageUri)
                    }
                    Log.d("DetailTransaksiScreen", "Loaded Firestore transaction: $transaction, imageUri: ${transaction?.imageUri}")
                } else {
                    Log.w("DetailTransaksiScreen", "Firestore transaction not found for ID: $transactionId")
                }
            }
        } catch (e: Exception) {
            Log.e("DetailTransaksiScreen", "Error loading transaction: $e")
            transaction = null
        } finally {
            isLoading = false
        }
    }

    fun deleteTransaction() {
        transaction?.id?.let { id ->
            if (id.startsWith("local_")) {
                val localId = id.removePrefix("local_").toLongOrNull()
                if (localId != null) {
                    viewModel.viewModelScope.launch {
                        try {
                            viewModel.localTransactionDao.deleteById(localId)
                            android.widget.Toast.makeText(
                                context,
                                "Transaksi lokal berhasil dihapus",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            navController.previousBackStackEntry?.savedStateHandle?.set("refresh", "true")
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Log.e("DetailTransaksiScreen", "Error deleting local transaction: $e")
                            android.widget.Toast.makeText(
                                context,
                                "Gagal menghapus transaksi: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Log.w("DetailTransaksiScreen", "Invalid local transaction ID for deletion: $id")
                }
            } else {
                db.collection("transactions")
                    .document(id)
                    .delete()
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(
                            context,
                            "Transaksi berhasil dihapus",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", "true")
                        navController.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Log.e("DetailTransaksiScreen", "Error deleting Firestore transaction: $e")
                        android.widget.Toast.makeText(
                            context,
                            "Gagal menghapus transaksi: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .padding(vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Navy
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detail Transaksi",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(
                color = Navy,
                modifier = Modifier.size(48.dp)
            )
        } else if (transaction != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .background(White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
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

                // Prioritaskan imageUri untuk offline
                transaction!!.imageUri?.let { imageUri ->
                    val file = File(imageUri)
                    if (file.exists()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Bukti Transaksi (Lokal)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Bukti Transaksi Lokal",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                                .clickable { showImageDialog = true },
                            onError = { error ->
                                Log.e("DetailTransaksiScreen", "Failed to load local image: $imageUri, error: ${error.result.throwable}")
                            }
                        )
                    } else {
                        Log.w("DetailTransaksiScreen", "Local image file not found: $imageUri")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Gambar lokal tidak ditemukan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ErrorRed,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        // Coba imageUrl sebagai fallback
                        transaction!!.imageUrl?.let { imageUrl ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bukti Transaksi (Online - Hanya Tersedia Online)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Navy,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Bukti Transaksi Online",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                                    .clickable { showImageDialog = true },
                                onError = { error ->
                                    Log.e("DetailTransaksiScreen", "Failed to load online image: $imageUrl, error: ${error.result.throwable}")
                                }
                            )
                        }
                    }
                } ?: transaction!!.imageUrl?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bukti Transaksi (Online - Hanya Tersedia Online)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Bukti Transaksi",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                            .clickable { showImageDialog = true },
                        onError = { error ->
                            Log.e("DetailTransaksiScreen", "Failed to load online image: $imageUrl, error: ${error.result.throwable}")
                        }
                    )
                } ?: run {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada bukti transaksi",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Shadow,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

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
                            // Prioritaskan imageUri untuk dialog
                            if (transaction!!.imageUri != null && File(transaction!!.imageUri).exists()) {
                                Log.d("DetailTransaksiScreen", "Loading dialog image from local: ${transaction!!.imageUri}")
                                AsyncImage(
                                    model = transaction!!.imageUri,
                                    contentDescription = "Bukti Transaksi (Lokal)",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                                    onError = { error ->
                                        Log.e("DetailTransaksiScreen", "Failed to load dialog local image: ${transaction!!.imageUri}, error: ${error.result.throwable}")
                                    }
                                )
                            } else if (transaction!!.imageUrl != null) {
                                Log.d("DetailTransaksiScreen", "Loading dialog image from online: ${transaction!!.imageUrl}")
                                AsyncImage(
                                    model = transaction!!.imageUrl,
                                    contentDescription = "Bukti Transaksi (Online)",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                                    onError = { error ->
                                        Log.e("DetailTransaksiScreen", "Failed to load dialog online image: ${transaction!!.imageUrl}, error: ${error.result.throwable}")
                                    }
                                )
                            } else {
                                Log.w("DetailTransaksiScreen", "No image available for dialog")
                                Text(
                                    text = "Gambar tidak tersedia",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ErrorRed
                                )
                            }
                        },
                        containerColor = White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        transaction?.let {
                            navController.navigate(Screen.EditTransaksi.createRoute(it.id))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Hapus",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            text = "Konfirmasi Hapus",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Navy
                        )
                    },
                    text = {
                        Text(
                            text = "Apakah Anda yakin ingin menghapus transaksi ini?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                deleteTransaction()
                                showDeleteDialog = false
                            }
                        ) {
                            Text(
                                text = "Hapus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .background(White, shape = RoundedCornerShape(8.dp))
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