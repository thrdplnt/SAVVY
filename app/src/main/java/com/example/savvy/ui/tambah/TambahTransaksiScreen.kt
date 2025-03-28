package com.example.savvy.ui.tambah

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.navigation.NavOptions
import com.example.savvy.ui.components.SavvyDropdownMenu
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.navigation.Screen
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.transaction.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TambahTransaksiScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // State untuk input form
    var transactionKind by remember { mutableStateOf("") } // Pemasukan atau Pengeluaran
    var type by remember { mutableStateOf("Tunai") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<Date?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Opsi untuk dropdown
    val transactionKinds = listOf("Pemasukan", "Pengeluaran")
    val transactionTypes = listOf("Tunai", "Non-Tunai", "Tabungan")
    val expenseCategories = listOf(
        "Makanan",
        "Transportasi",
        "Hiburan",
        "Pendidikan",
        "Tagihan",
        "Kesehatan",
        "Belanja",
        "Uang Keluar"
    )

    // Date picker
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            date = calendar.time
            dateText = SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(date!!)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tambah Transaksi",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Dropdown Jenis Transaksi (Tunai/Non-Tunai)
        SavvyDropdownMenu(
            label = "Pilih Dompetku",
            items = transactionTypes,
            selectedItem = type,
            onItemSelected = { type = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input Jumlah Uang (dipindah ke atas)
        SavvyTextField(
            value = amount,
            onValueChange = { newValue ->
                // Hanya izinkan angka dan tanda desimal
                if (newValue.all { it.isDigit() || it == '.' }) {
                    amount = newValue
                }
            },
            label = "Masukkan Jumlah Uang",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown Pemasukan/Pengeluaran (dipindah setelah jumlah)
        SavvyDropdownMenu(
            label = "Pilih Jenis Transaksi",
            items = transactionKinds,
            selectedItem = transactionKind,
            onItemSelected = {
                transactionKind = it
                if (it == "Pemasukan") {
                    category = "Pemasukan"
                } else {
                    category = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown Kategori
        SavvyDropdownMenu(
            label = "Pilih Kategori",
            items = if (transactionKind == "Pemasukan") listOf("Pemasukan") else expenseCategories,
            selectedItem = category,
            onItemSelected = { category = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = transactionKind != "Pemasukan"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input Catatan
        SavvyTextField(
            value = note,
            onValueChange = { note = it },
            label = "Tulis Catatan",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input Tanggal (Manual atau via DatePicker)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SavvyTextField(
                value = dateText,
                onValueChange = { newValue ->
                    dateText = newValue
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id"))
                        sdf.isLenient = false
                        date = sdf.parse(newValue)
                    } catch (e: Exception) {
                        date = null
                    }
                },
                label = "Tanggal (dd/mm/yyyy)",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Pilih Tanggal",
                    tint = Navy
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder untuk Tambahkan Gambar
        OutlinedButton(
            onClick = { /* Placeholder untuk menambahkan gambar */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Tambahkan Gambar",
                color = Navy,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Save (Hanya bagian ini yang diubah)
        Button(
            onClick = {
                // Validasi input
                if (transactionKind.isEmpty() || type.isEmpty() || amount.isEmpty() || category.isEmpty() || date == null) {
                    Toast.makeText(context, "Harap isi semua kolom wajib", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val amountLong = amount.toLongOrNull()
                if (amountLong == null || amountLong <= 0) {
                    Toast.makeText(context, "Jumlah harus berupa angka positif", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val user = auth.currentUser
                if (user == null) {
                    Toast.makeText(context, "Pengguna tidak ditemukan, harap login", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Login.route)
                    return@Button
                }

                isLoading = true

                // Buat objek transaksi
                val transaction = Transaction(
                    type = type,
                    amount = amountLong,
                    category = category,
                    note = note,
                    date = date,
                    userId = user.uid
                )

                // Simpan transaksi ke Firestore
                db.collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener { documentReference ->
                        isLoading = false
                        Toast.makeText(context, "Tambah Transaksi Sukses Dibuat", Toast.LENGTH_SHORT).show() // Ubah pesan sukses
                        navController.navigate(
                            Screen.Riwayat.route,
                            navOptions = NavOptions.Builder()
                                .setPopUpTo(Screen.Tambah.route, inclusive = true)
                                .build()
                        )
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Gagal menambahkan transaksi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Beige,
                contentColor = Navy
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Navy,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}