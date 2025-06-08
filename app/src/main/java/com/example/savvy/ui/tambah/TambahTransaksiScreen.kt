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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.savvy.data.LocalTransaction
import com.example.savvy.data.Screen
import com.example.savvy.data.Transaction
import com.example.savvy.ui.components.SavvyDropdownMenu
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.savvy.ui.wallet.WalletViewModel

@Composable
fun TambahTransaksiScreen(
    navController: NavController,
    viewModel: TambahTransaksiViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel(),
    transactionId: String? = null
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val walletsState by viewModel.wallets.collectAsState()

    var transactionKind by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Tunai") } // Ini adalah sumber dana/dompet
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var dateText by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(date)) }

    var isLoading by remember { mutableStateOf(false) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrlFromDb by remember { mutableStateOf<String?>(null) }
    var existingLocalImageUriForDisplay by remember { mutableStateOf<String?>(null) }

    var isEditMode by remember { mutableStateOf(transactionId != null) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var isImageRemovedByUser by remember { mutableStateOf(false) }

    var loadedTransactionForEdit by remember { mutableStateOf<Transaction?>(null) }
    var initialClientGeneratedIdForEdit by remember { mutableStateOf<String?>(null) }
    var initialLocalDbIdForEdit by remember { mutableStateOf<Long?>(null) }
    var initialFirestoreIdForEdit by remember { mutableStateOf<String?>(null) }

    val transactionWalletTypes = listOf("Tunai", "Non-Tunai", "Tabungan")
//    var selectedWalletType by remember { mutableStateOf(type) } // State untuk dropdown dompet

    var selectedWalletIdState by remember { mutableStateOf("") } // Menyimpan ID dompet yang dipilih
    var selectedWalletNameState by remember { mutableStateOf("Pilih Dompet") } // Menyimpan NAMA dompet yang dipilih

    val transactionKinds = listOf("Pemasukan", "Pengeluaran")
    val expenseCategories = listOf(
        "Makanan", "Transportasi", "Hiburan", "Pendidikan",
        "Tagihan", "Kesehatan", "Belanja", "Uang Keluar"
    )

    // Efek untuk menginisialisasi pilihan dompet saat daftar dompet berubah atau saat mode edit
    LaunchedEffect(walletsState, loadedTransactionForEdit, isEditMode) {
        if (!isEditMode && walletsState.isNotEmpty() && selectedWalletIdState.isBlank()) {
            // Set default untuk mode tambah baru jika dompet sudah dimuat
            val defaultWallet = walletsState.find { it.name.equals("Tunai", ignoreCase = true) } ?: walletsState.firstOrNull()
            defaultWallet?.let {
                selectedWalletIdState = it.id
                selectedWalletNameState = it.name
            }
        } else if (isEditMode && loadedTransactionForEdit != null) {
            // Jika mode edit dan data transaksi sudah dimuat
            val currentWallet = walletsState.find { it.id == loadedTransactionForEdit!!.walletId }
            if (currentWallet != null) {
                selectedWalletIdState = currentWallet.id
                selectedWalletNameState = currentWallet.name
            } else if (!loadedTransactionForEdit!!.walletId.isNullOrBlank()){
                // Jika walletId dari transaksi lama ada tapi tidak ditemukan di daftar dompet saat ini
                selectedWalletNameState = loadedTransactionForEdit!!.type // type di Transaction adalah nama dompet lama
                selectedWalletIdState = loadedTransactionForEdit!!.walletId
            }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId == null) {
            isEditMode = false
            transactionKind = ""
            type = transactionWalletTypes.firstOrNull() ?: "Tunai"
            selectedWalletIdState = type
            selectedWalletNameState = ""
            amount = ""
            category = ""
            note = ""
            date = Date()
            dateText = SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(date)
            newImageUri = null
            existingImageUrlFromDb = null
            existingLocalImageUriForDisplay = null
            isImageRemovedByUser = false
            loadedTransactionForEdit = null
            initialClientGeneratedIdForEdit = null
            initialLocalDbIdForEdit = null
            initialFirestoreIdForEdit = null
            return@LaunchedEffect
        }

        isEditMode = true
        isLoading = true
        Log.d("TambahScreen", "Edit mode. TransactionID from route: $transactionId")

        if (transactionId.startsWith("local_")) {
            val localId = transactionId.removePrefix("local_").toLongOrNull()
            if (localId != null) {
                initialLocalDbIdForEdit = localId
                viewModel.getLocalTransactionByDbId(localId) { localTx ->
                    if (localTx != null) {
                        loadedTransactionForEdit = Transaction(
                            id = localTx.firestoreId ?: transactionId,
                            clientGeneratedId = localTx.clientGeneratedId,
                            userId = localTx.userId,
                            type = localTx.type,
                            amount = localTx.amount,
                            category = localTx.category,
                            note = localTx.note,
                            date = localTx.date,
                            imageUrl = localTx.imageUrl,
                            imageUri = localTx.imageUri,
                            walletId = localTx.walletId ?: localTx.type
                        )
                        initialClientGeneratedIdForEdit = localTx.clientGeneratedId
                        initialFirestoreIdForEdit = localTx.firestoreId

                        transactionKind = if (localTx.category == "Pemasukan") "Pemasukan" else "Pengeluaran"
                        type = localTx.type
                        selectedWalletIdState = localTx.walletId ?: localTx.type
                        amount = localTx.amount.toString()
                        category = localTx.category
                        note = localTx.note
                        date = localTx.date
                        dateText = SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(date)
                        existingImageUrlFromDb = localTx.imageUrl
                        existingLocalImageUriForDisplay = localTx.imageUri
                        Log.d("TambahScreen", "Loaded from Room: $localTx")
                    } else {
                        Toast.makeText(context, "Transaksi lokal tidak ditemukan", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    isLoading = false
                }
            } else {
                Toast.makeText(context, "ID transaksi lokal tidak valid", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                isLoading = false
            }
        } else { // Ini adalah Firestore ID
            initialFirestoreIdForEdit = transactionId
            FirebaseFirestore.getInstance().collection("transactions").document(transactionId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val txFromFirestore = document.toObject(Transaction::class.java)?.copy(id = document.id)
                        txFromFirestore?.let { trans ->
                            loadedTransactionForEdit = trans
                            initialClientGeneratedIdForEdit = trans.clientGeneratedId

                            transactionKind = if (trans.category == "Pemasukan") "Pemasukan" else "Pengeluaran"
                            type = trans.type
                            selectedWalletIdState = trans.walletId.ifBlank { trans.type }
                            amount = trans.amount.toString()
                            category = trans.category
                            note = trans.note
                            date = trans.date ?: Date()
                            dateText = SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(date)
                            existingImageUrlFromDb = trans.imageUrl
                            Log.d("TambahScreen", "Loaded from Firestore: $trans")
                        }
                    } else {
                        Toast.makeText(context, "Transaksi tidak ditemukan di server", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    navController.popBackStack()
                }
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth); date = calendar.time
            dateText = SimpleDateFormat("dd/MM/yyyy", Locale("id")).format(date)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { newImageUri = it; isImageRemovedByUser = false; existingImageUrlFromDb = null; existingLocalImageUriForDisplay = null }
    }
    val tempCameraImageUri = remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempCameraImageUri.value?.let { newImageUri = it; isImageRemovedByUser = false; existingImageUrlFromDb = null; existingLocalImageUriForDisplay = null }
    }
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "savvy_temp_cam_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraImageUri.value = photoUri
            takePictureLauncher.launch(photoUri)
        } else Toast.makeText(context, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(White).padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Untuk menyeimbangkan jika ada IconButton
        ) {
            if (isEditMode) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(48.dp)) { // Beri ukuran agar konsisten
                    Icon(Icons.Default.Close, "Batal", tint = Navy)
                }
            } else {
                Spacer(Modifier.size(48.dp)) // Spacer kosong untuk menyeimbangkan
            }
            Text(
                text = if (isEditMode) "Edit Transaksi" else "Tambah Transaksi",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f) // Agar teks mengisi ruang dan bisa di tengah
            )
            Spacer(Modifier.size(48.dp)) // Spacer kosong untuk menyeimbangkan sisi kanan
        }

        SavvyDropdownMenu(
            label = "Pilih Dompetku",
            items = walletsState.map { it.name }.ifEmpty { listOf("Memuat dompet...") }, // Daftar nama dompet
            selectedItem = if (walletsState.isEmpty() && selectedWalletIdState.isBlank()) "Memuat..." else selectedWalletNameState,
            onItemSelected = { walletName ->
                selectedWalletNameState = walletName
                selectedWalletIdState = walletsState.find { it.name == walletName }?.id ?: ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = walletsState.isNotEmpty() // Aktifkan jika dompet sudah dimuat
        )
        Spacer(Modifier.height(16.dp))

        SavvyTextField(
            value = amount,
            onValueChange = { v -> if (v.all { it.isDigit() }) amount = v },
            label = "Masukkan Jumlah Uang",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Number // Menggunakan parameter KeyboardType
        )
        Spacer(Modifier.height(16.dp))

        SavvyDropdownMenu(
            label = "Pilih Jenis Transaksi", items = transactionKinds, selectedItem = transactionKind,
            onItemSelected = { transactionKind = it; category = if (it == "Pemasukan") "Pemasukan" else "" },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        SavvyDropdownMenu(
            label = "Pilih Kategori",
            items = if (transactionKind == "Pemasukan") listOf("Pemasukan") else expenseCategories,
            selectedItem = category, onItemSelected = { category = it }, modifier = Modifier.fillMaxWidth(),
            enabled = transactionKind != "Pemasukan" || category == "Pemasukan"
        )
        Spacer(Modifier.height(16.dp))

        SavvyTextField(
            value = note,
            onValueChange = { note = it },
            label = "Tulis Catatan",
            modifier = Modifier.fillMaxWidth(),
            singleLine = false // Izinkan multiple lines untuk catatan
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SavvyTextField(
                value = dateText,
                onValueChange = { /* Dikosongkan karena readOnly dan dihandle picker */ },
                label = "Tanggal (dd/mm/yyyy)",
                modifier = Modifier.weight(1f),
                readOnly = true, // Menggunakan parameter baru dari SavvyTextField
                onClickAction = { datePickerDialog.show() } // Menggunakan parameter baru
            )
            IconButton(onClick = { datePickerDialog.show() }) { // Tombol ikon tetap ada
                Icon(Icons.Default.DateRange, "Pilih Tanggal", tint = Navy)
            }
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showPhotoOptionsDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp).drawBehind {
                val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                drawRoundRect(color = Navy, style = stroke, cornerRadius = CornerRadius(8.dp.toPx()))
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Navy),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = when {
                    newImageUri != null -> "Gambar Baru Dipilih"
                    existingLocalImageUriForDisplay != null && !isImageRemovedByUser -> "Lihat Gambar Lokal"
                    existingImageUrlFromDb != null && !isImageRemovedByUser -> "Lihat Gambar Cloud"
                    else -> "Tambahkan Gambar"
                },
                color = Navy, fontSize = 16.sp
            )
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (transactionKind.isEmpty()) { Toast.makeText(context, "Jenis transaksi harus dipilih", Toast.LENGTH_SHORT).show(); return@Button }
                if (selectedWalletIdState.isEmpty()) { Toast.makeText(context, "Dompet harus dipilih", Toast.LENGTH_SHORT).show(); return@Button } // Validasi selectedWalletType
                if (amount.isEmpty()) { Toast.makeText(context, "Jumlah uang harus diisi", Toast.LENGTH_SHORT).show(); return@Button }
                val amountLongParsed = amount.toLongOrNull()
                if (amountLongParsed == null || amountLongParsed <= 0) { Toast.makeText(context, "Jumlah harus angka positif", Toast.LENGTH_SHORT).show(); return@Button }
                if (category.isEmpty()) { Toast.makeText(context, "Kategori harus dipilih", Toast.LENGTH_SHORT).show(); return@Button }

                val user = auth.currentUser
                if (user == null) { Toast.makeText(context, "Harap login ulang", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.Login.route); return@Button }

                isLoading = true

                // PENTING: clientGeneratedId di sini harus konsisten.
                // Untuk transaksi baru, generate UUID baru.
                // Untuk edit, gunakan initialClientGeneratedIdForEdit.
                val finalClientGeneratedId = if (isEditMode) {
                    initialClientGeneratedIdForEdit.takeIf { !it.isNullOrBlank() } ?: loadedTransactionForEdit?.clientGeneratedId ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }


                val transactionDataForViewModel = Transaction(
                    userId = user.uid,
                    type = selectedWalletNameState, // Gunakan selectedWalletType untuk type dompet
                    amount = amountLongParsed,
                    category = category,
                    note = note,
                    date = date,
                    walletId = selectedWalletIdState, // walletId juga diisi dengan selectedWalletType
                    imageUrl = if (isEditMode && newImageUri == null && !isImageRemovedByUser) existingImageUrlFromDb else null,
                    clientGeneratedId = finalClientGeneratedId, // PASTIKAN clientGeneratedId ada
                    imageUri = null
                )

                if (isEditMode) {
                    viewModel.updateTransaction(
                        existingFirestoreId = initialFirestoreIdForEdit,
                        existingLocalId = initialLocalDbIdForEdit,
                        existingClientGeneratedId = finalClientGeneratedId, // Gunakan finalClientGeneratedId yang sudah pasti non-nullable
                        transactionInput = transactionDataForViewModel,
                        newImageUri = newImageUri,
                        isImageRemoved = isImageRemovedByUser,
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            navController.previousBackStackEntry?.savedStateHandle?.set("refresh_riwayat", true)
                            navController.popBackStack()
                        },
                        onFailure = { e ->
                            isLoading = false; Toast.makeText(context, "Gagal memperbarui: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    viewModel.saveTransaction(
                        transactionInput = transactionDataForViewModel,
                        imageUri = newImageUri,
                        onSuccess = { firestoreId ->
                            isLoading = false
                            val message = if (firestoreId != null) "Tambah Transaksi Sukses" else "Transaksi disimpan lokal"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }; launchSingleTop = true
                            }
                        },
                        onFailure = { e ->
                            isLoading = false; Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Beige, contentColor = Navy)
        ) {
            if (isLoading) CircularProgressIndicator(color = Navy, modifier = Modifier.size(24.dp))
            else Text(if (isEditMode) "Simpan Perubahan" else "Simpan", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(80.dp))
    }

    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { Text("Pilih Opsi Gambar", style = MaterialTheme.typography.headlineMedium, color = Navy) },
            text = {
                Column {
                    TextButton(onClick = { showPhotoOptionsDialog = false; pickImageLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Pilih dari Galeri", style = MaterialTheme.typography.bodyLarge, color = Navy)
                    }
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val photoFile = File(context.cacheDir, "savvy_temp_cam_${System.currentTimeMillis()}.jpg")
                                val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                tempCameraImageUri.value = photoUri
                                takePictureLauncher.launch(photoUri)
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ambil Foto", style = MaterialTheme.typography.bodyLarge, color = Navy) }
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false; newImageUri = null; existingImageUrlFromDb = null
                            existingLocalImageUriForDisplay = null; isImageRemovedByUser = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newImageUri != null || existingImageUrlFromDb != null || existingLocalImageUriForDisplay != null
                    ) {
                        Text("Hapus", style = MaterialTheme.typography.bodyLarge, color = if (newImageUri != null || existingImageUrlFromDb != null || existingLocalImageUriForDisplay != null) MaterialTheme.colorScheme.error else Color.Gray)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPhotoOptionsDialog = false }) { Text("Batal", style = MaterialTheme.typography.bodyLarge, color = Navy) } },
            containerColor = White
        )
    }
}