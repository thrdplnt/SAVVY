package com.example.savvy.ui.tambah

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.savvy.data.Screen
import com.example.savvy.data.Transaction
import com.example.savvy.ui.components.SavvyDropdownMenu
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TambahTransaksiScreen(
    navController: NavController,
    viewModel: TambahTransaksiViewModel = hiltViewModel(),
    transactionId: String? = null
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // State untuk input form
    var transactionKind by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Tunai") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateText by remember {
        mutableStateOf(
            SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(Date())
        )
    }
    var date by remember { mutableStateOf(Date()) } // Default ke tanggal saat ini
    var isLoading by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    var isEditMode by remember { mutableStateOf(transactionId != null) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var isImageRemoved by remember { mutableStateOf(false) }

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

    // Ambil data transaksi jika dalam mode edit
    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            FirebaseFirestore.getInstance()
                .collection("transactions")
                .document(transactionId)
                .get()
                .addOnSuccessListener { document ->
                    val transaction = document.toObject(Transaction::class.java)
                    transaction?.let { trans ->
                        transactionKind = if (trans.category == "Pemasukan") "Pemasukan" else "Pengeluaran"
                        type = trans.type
                        amount = trans.amount.toString()
                        category = trans.category
                        note = trans.note
                        date = trans.date ?: Date()
                        dateText = date.let { d ->
                            SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(d)
                        }
                        existingImageUrl = trans.imageUrl
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal memuat data transaksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Date picker
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            date = calendar.time
            dateText = SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(date)
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
            isImageRemoved = false
        }
    }

    // Launcher untuk mengambil foto dari kamera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let {
                isImageRemoved = false
            }
        }
    }

    // Launcher untuk meminta izin kamera
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "temp_camera_image.jpg")
            val photoUri = FileProvider.getUriForFile(
                context,
                "com.example.savvy.fileprovider",
                photoFile
            )
            imageUri = photoUri

            context.grantUriPermission(
                "com.android.camera",
                photoUri,
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            takePictureLauncher.launch(photoUri)
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Added vertical scrolling
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Baris untuk tombol "X" dan judul
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(start = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Batal",
                        tint = Navy
                    )
                }
                Text(
                    text = "Edit Transaksi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
            } else {
                Text(
                    text = "Tambah Transaksi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }

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
                        date = sdf.parse(newValue) ?: Date()
                    } catch (e: Exception) {
                        date = Date()
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
            onClick = { showPhotoOptionsDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
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
                containerColor = Color.Transparent,
                contentColor = Navy
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            )
        ) {
            Text(
                text = when {
                    imageUri != null -> "Gambar Dipilih"
                    existingImageUrl != null && !isImageRemoved -> "Gambar Sudah Ada"
                    else -> "Tambahkan Gambar"
                },
                color = Navy,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Save
        Button(
            onClick = {
                // Validasi input
                if (transactionKind.isEmpty()) {
                    Toast.makeText(context, "Jenis transaksi harus dipilih", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (type.isEmpty()) {
                    Toast.makeText(context, "Dompet harus dipilih", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (amount.isEmpty()) {
                    Toast.makeText(context, "Jumlah uang harus diisi", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (category.isEmpty()) {
                    Toast.makeText(context, "Kategori harus dipilih", Toast.LENGTH_SHORT).show()
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

                val transaction = Transaction(
                    type = type,
                    amount = amountLong,
                    category = category,
                    note = note,
                    date = date,
                    userId = user.uid,
                    imageUrl = if (isImageRemoved) null else existingImageUrl
                )

                if (isEditMode) {
                    viewModel.updateTransaction(
                        transactionId = transactionId,
                        localTransactionId = null, // Tidak digunakan untuk saat ini
                        transaction = transaction,
                        imageUri = if (isImageRemoved) null else imageUri,
                        onSuccess = {
                            Log.d("TambahTransaksiScreen", "Update transaction success")
                            isLoading = false
                            Toast.makeText(context, "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            navController.previousBackStackEntry?.savedStateHandle?.set("refresh", "true")
                            navController.popBackStack()
                        },
                        onFailure = { e ->
                            Log.e("TambahTransaksiScreen", "Update transaction failed: $e")
                            isLoading = false
                            Toast.makeText(context, "Gagal memperbarui transaksi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    viewModel.saveTransaction(
                        transaction = transaction,
                        imageUri = if (isImageRemoved) null else imageUri,
                        onSuccess = { firestoreId ->
                            Log.d("TambahTransaksiScreen", "Save transaction success, firestoreId: $firestoreId")
                            isLoading = false
                            val message = if (firestoreId != null) {
                                "Tambah Transaksi Sukses Dibuat"
                            } else {
                                "Transaksi disimpan secara lokal dan akan disinkronkan saat online"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            navController.navigate(
                                Screen.Riwayat.route,
                                navOptions = NavOptions.Builder()
                                    .setPopUpTo(Screen.Tambah.route, inclusive = true)
                                    .build()
                            )
                        },
                        onFailure = { e ->
                            Log.e("TambahTransaksiScreen", "Save transaction failed: $e")
                            isLoading = false
                            Toast.makeText(context, "Gagal menyimpan transaksi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
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
                    text = if (isEditMode) "Simpan Perubahan" else "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Spacer to ensure content isn't clipped by bottom navigation or keyboard
        Spacer(modifier = Modifier.height(80.dp))
    }

    // Dialog untuk opsi foto
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Text(
                    "Pilih Opsi Gambar",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Navy
                )
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            pickImageLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pilih dari Galeri",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                    }
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                val photoFile = File(context.cacheDir, "temp_camera_image.jpg")
                                val photoUri = FileProvider.getUriForFile(
                                    context,
                                    "com.example.savvy.fileprovider",
                                    photoFile
                                )
                                imageUri = photoUri

                                context.grantUriPermission(
                                    "com.android.camera",
                                    photoUri,
                                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )

                                takePictureLauncher.launch(photoUri)
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ambil Foto",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Navy
                        )
                    }
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            imageUri = null
                            existingImageUrl = null
                            isImageRemoved = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = imageUri != null || existingImageUrl != null
                    ) {
                        Text(
                            text = "Hapus",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (imageUri != null || existingImageUrl != null) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text(
                        text = "Batal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy
                    )
                }
            },
            containerColor = White
        )
    }
}