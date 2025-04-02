package com.example.savvy.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.savvy.data.Transaction
import com.example.savvy.ui.theme.ErrorRed
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.Typography
import com.example.savvy.ui.theme.White
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailTransaksi(
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Detail Transaksi",
                style = Typography.headlineMedium,
                color = Navy
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Kategori
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Kategori",
                        style = Typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction.category,
                        style = Typography.bodyMedium,
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
                        style = Typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(transaction.amount)}",
                        style = Typography.bodyMedium,
                        color = if (transaction.category == "Pemasukan") Navy else ErrorRed
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
                        style = Typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction.type,
                        style = Typography.bodyMedium,
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
                        style = Typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction.date?.let { dateFormat.format(it) } ?: "Tanggal Tidak Diketahui",
                        style = Typography.bodyMedium,
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
                        style = Typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction.note.ifEmpty { "Tidak ada catatan" },
                        style = Typography.bodyMedium,
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
                        style = Typography.bodyLarge,
                        color = Navy
                    )
                    Text(
                        text = transaction.walletId.ifEmpty { "Tidak ada ID Dompet" },
                        style = Typography.bodyMedium,
                        color = Navy
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Gambar (jika ada)
                transaction.imageUrl?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bukti Transaksi",
                        style = Typography.bodyLarge,
                        color = Navy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Bukti Transaksi",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Tutup",
                    style = Typography.bodyMedium,
                    color = Navy
                )
            }
        },
        containerColor = White
    )
}