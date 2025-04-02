package com.example.savvy.ui.tambah

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.savvy.data.SupabaseStorageUploader
import com.example.savvy.ui.components.SavvyDropdownMenu
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.data.Screen
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.data.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius

// Fungsi untuk memeriksa koneksi internet
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun TambahTransaksiScreen(
    navController: NavController,
    uploader: SupabaseStorageUploader // Inject uploader
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    // State untuk input form
    var transactionKind by remember { mutableStateOf("") } // Pemasukan atau Pengeluaran
    var type by remember { mutableStateOf("Tunai") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<Date?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) } // State untuk menyimpan URI gambar

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

    // Launcher untuk memilih gambar dari galeri
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = it
        }
    }

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

        // Input Jumlah Uang
        SavvyTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' }) {
                    amount = newValue
                }
            },
            label = "Masukkan Jumlah Uang",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown Pemasukan/Pengeluaran
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

        // Tombol Tambahkan Gambar
        Button(
            onClick = {
                pickImageLauncher.launch("image/*")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // Perbesar tinggi tombol
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val cornerRadius = 8.dp.toPx()
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    drawRoundRect(
                        color = Navy,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = pathEffect
                        ),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                },
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // Background transparan
                contentColor = Navy
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp // Hilangkan shadow
            )
        ) {
            Text(
                text = if (imageUri != null) "Gambar Dipilih" else "Tambahkan Gambar",
                color = Navy,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Save
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

                // Periksa koneksi internet
                if (!isNetworkAvailable(context)) {
                    Toast.makeText(context, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                // Jika ada gambar yang dipilih, unggah ke Supabase Storage
                // Inside the Button's onClick lambda
                if (imageUri != null) {
                    coroutineScope.launch {
                        try {
                            // Konversi URI ke File dengan kompresi
                            val inputStream = context.contentResolver.openInputStream(imageUri!!)
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()

                            // Kompresi gambar
                            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                            val compressedByteArray = byteArrayOutputStream.toByteArray()

                            // Simpan ke file sementara
                            val file = File(context.cacheDir, "temp_image.jpg")
                            file.writeBytes(compressedByteArray)

                            // Unggah ke Supabase Storage
                            val destinationFileName = "images/${System.currentTimeMillis()}_image.jpg"
                            val imageUrl = uploader.uploadImage(file, destinationFileName) // Ensure correct method name

                            if (imageUrl != null) {
                                // Buat objek transaksi dengan URL gambar
                                val transaction = Transaction(
                                    type = type,
                                    amount = amountLong,
                                    category = category,
                                    note = note,
                                    date = date,
                                    userId = user.uid,
                                    imageUrl = imageUrl
                                )

                                // Simpan transaksi ke Firestore
                                db.collection("transactions")
                                    .add(transaction)
                                    .addOnSuccessListener { documentReference ->
                                        isLoading = false
                                        Toast.makeText(context, "Tambah Transaksi Sukses Dibuat", Toast.LENGTH_SHORT).show()
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
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Gagal mengunggah gambar ke Supabase", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Jika tidak ada gambar, simpan transaksi tanpa imageUrl
                    val transaction = Transaction(
                        type = type,
                        amount = amountLong,
                        category = category,
                        note = note,
                        date = date,
                        userId = user.uid
                    )

                    db.collection("transactions")
                        .add(transaction)
                        .addOnSuccessListener { documentReference ->
                            isLoading = false
                            Toast.makeText(context, "Tambah Transaksi Sukses Dibuat", Toast.LENGTH_SHORT).show()
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