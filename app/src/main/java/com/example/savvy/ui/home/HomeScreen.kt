package com.example.savvy.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savvy.R
import com.example.savvy.ui.components.AnalysisItem
import com.example.savvy.data.Screen
import com.example.savvy.ui.theme.Navy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.Image

@Composable
fun HomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val keyboardController = LocalSoftwareKeyboardController.current

    // State untuk data saldo dan transaksi
    var totalSaldo by rememberSaveable { mutableLongStateOf(0L) }
    var saldoTunai by rememberSaveable { mutableLongStateOf(0L) }
    var saldoNonTunai by rememberSaveable { mutableLongStateOf(0L) }
    var saldoTabungan by rememberSaveable { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var showDompetDialog by remember { mutableStateOf(false) }
    var isSaldoVisible by remember { mutableStateOf(true) } // State untuk visibilitas saldo

    // State untuk analisis transaksi per kategori (dalam sebulan)
    val categoryData = remember { mutableStateMapOf<String, Pair<Int, Long>>() }
    var totalPengeluaran by rememberSaveable { mutableLongStateOf(0L) }

    // State untuk semua transaksi
    val allTransactions = remember { mutableStateListOf<Map<String, Any>>() }

    // State untuk pencarian
    var searchQuery by remember { mutableStateOf("") }
    val filteredTransactions = remember { mutableStateListOf<Map<String, Any>>() }

    // Warna untuk pie chart
    val categoryColors = mapOf(
        "Makanan" to Color(0xFF6256D1),
        "Transportasi" to Color(0xFF83E46F),
        "Hiburan" to Color(0xFF4894FF),
        "Pendidikan" to Color(0xFFFFD300),
        "Tagihan" to Color(0xFFFF4A4A),
        "Kesehatan" to Color(0xFF9DCFFF),
        "Belanja" to Color(0xFFFF458A),
        "Uang Keluar" to Color(0xFF76E7E7)
    )

    // Ambil data transaksi dari Firestore
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user == null) {
            navController.navigate(Screen.Login.route)
            return@LaunchedEffect
        }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        db.collection("transactions")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var tunai = 0L
                var nonTunai = 0L
                var tabungan = 0L
                val categoryMap = mutableMapOf<String, Pair<Int, Long>>()
                allTransactions.clear()

                for (document in querySnapshot.documents) {
                    val type = document.getString("type") ?: ""
                    val amount = document.getLong("amount") ?: 0L
                    val category = document.getString("category") ?: ""
                    val date = document.getTimestamp("date")?.toDate()
                    val note = document.getString("note") ?: ""

                    // Simpan semua transaksi dengan tipe yang sesuai
                    val transaction = mapOf<String, Any>(
                        "type" to type,
                        "amount" to amount,
                        "category" to category,
                        "date" to (date ?: ""),
                        "note" to note
                    )
                    allTransactions.add(transaction)

                    // Hitung saldo per dompet
                    when (type) {
                        "Tunai" -> if (category == "Pemasukan") tunai += amount else tunai -= amount
                        "Non-Tunai" -> if (category == "Pemasukan") nonTunai += amount else nonTunai -= amount
                        "Tabungan" -> if (category == "Pemasukan") tabungan += amount else tabungan -= amount
                    }

                    // Hanya ambil transaksi untuk bulan saat ini untuk analisis
                    if (date != null) {
                        calendar.time = date
                        val transactionMonth = calendar.get(Calendar.MONTH)
                        val transactionYear = calendar.get(Calendar.YEAR)

                        if (transactionMonth == currentMonth && transactionYear == currentYear && category != "Pemasukan") {
                            val currentData = categoryMap[category] ?: Pair(0, 0L)
                            categoryMap[category] = Pair(currentData.first + 1, currentData.second + amount)
                        }
                    }
                }

                saldoTunai = tunai
                saldoNonTunai = nonTunai
                saldoTabungan = tabungan
                totalSaldo = tunai + nonTunai + tabungan
                categoryData.putAll(categoryMap)
                totalPengeluaran = categoryMap.values.sumOf { it.second }
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                android.widget.Toast.makeText(
                    context,
                    "Gagal mengambil data transaksi: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Fungsi untuk menangani pencarian
    fun performSearch() {
        if (searchQuery.isNotEmpty()) {
            filteredTransactions.clear()
            val query = searchQuery.lowercase()
            allTransactions.forEach { transaction ->
                val category = (transaction["category"] as? String)?.lowercase() ?: ""
                val note = (transaction["note"] as? String)?.lowercase() ?: ""
                val date = transaction["date"]
                val dateString = if (date is Date) {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date).lowercase()
                } else {
                    ""
                }

                if (category.contains(query) || note.contains(query) || dateString.contains(query)) {
                    filteredTransactions.add(transaction)
                }
            }
            keyboardController?.hide() // Sembunyikan keyboard
        } else {
            filteredTransactions.clear()
        }
    }

    // Dialog untuk menampilkan semua dompet
    if (showDompetDialog) {
        AlertDialog(
            onDismissRequest = { showDompetDialog = false },
            title = { Text("Dompetku", color = Navy, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Tunai: Rp ${NumberFormat.getNumberInstance(Locale("id")).format(saldoTunai)}")
                    Text("Non-Tunai: Rp ${NumberFormat.getNumberInstance(Locale("id")).format(saldoNonTunai)}")
                    Text("Tabungan: Rp ${NumberFormat.getNumberInstance(Locale("id")).format(saldoTabungan)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDompetDialog = false }) {
                    Text("Tutup", color = Navy)
                }
            }
        )
    }

    // Gunakan Column langsung tanpa Scaffold
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Savvy di sebelah kiri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_savvy_small),
                contentDescription = "Savvy Logo",
                modifier = Modifier.size(120.dp, 40.dp)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .onKeyEvent { event ->
                    if (event.key == Key.Enter) {
                        performSearch()
                        true
                    } else {
                        false
                    }
                },
            placeholder = { Text("Search") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Navy
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Perform Search",
                        tint = Navy,
                        modifier = Modifier
                            .clickable { performSearch() }
                            .size(24.dp)
                    )
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Navy,
                unfocusedBorderColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { performSearch() }
            )
        )

        // Tampilkan hasil pencarian jika ada
        if (filteredTransactions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Hasil Pencarian",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    filteredTransactions.forEach { transaction ->
                        val amount = transaction["amount"] as? Long ?: 0L
                        val category = transaction["category"] as? String ?: "Unknown"
                        val date = transaction["date"]
                        val dateString = if (date is Date) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                        } else {
                            "Unknown Date"
                        }
                        val note = transaction["note"] as? String ?: "No note"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy
                                )
                                Text(
                                    text = "Tanggal: $dateString",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Catatan: $note",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "-Rp ${NumberFormat.getNumberInstance(Locale("id")).format(amount)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else {
            // Total Saldo dengan Ikon Mata (posisi di sebelah kiri)
            if (isLoading) {
                CircularProgressIndicator(
                    color = Navy,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start // Geser ke kiri
                ) {
                    Text(
                        text = if (isSaldoVisible) "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalSaldo)}" else "****",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isSaldoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Saldo Visibility",
                        tint = Navy,
                        modifier = Modifier
                            .clickable { isSaldoVisible = !isSaldoVisible }
                            .size(24.dp)
                    )
                }
                Text(
                    text = "Total saldo",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start // Teks "Total saldo" juga di sebelah kiri
                )

                // Dompetku (di dalam kotak biru dengan shadow)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE6F0FA)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Dompetku",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )
                            Text(
                                text = "Lihat selengkapnya",
                                fontSize = 14.sp,
                                color = Navy,
                                modifier = Modifier
                                    .clickable { showDompetDialog = true }
                                    .padding(start = 8.dp),
                                textDecoration = TextDecoration.Underline // Garis bawah
                            )
                        }
                        // Garis putih di bawah "Dompetku"
                        Divider(
                            color = Color.White,
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tunai",
                                fontSize = 16.sp,
                                color = Navy
                            )
                            Text(
                                text = if (isSaldoVisible) "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(saldoTunai)}" else "****",
                                fontSize = 16.sp,
                                color = Navy,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tabungan",
                                fontSize = 16.sp,
                                color = Navy
                            )
                            Text(
                                text = if (isSaldoVisible) "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(saldoTabungan)}" else "****",
                                fontSize = 16.sp,
                                color = Navy,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Pie Chart (di dalam kotak putih dengan shadow)
                if (totalPengeluaran > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(150.dp)) {
                                var startAngle = 0f
                                categoryData.forEach { (category, data) ->
                                    val sweepAngle = (data.second.toFloat() / totalPengeluaran.toFloat()) * 360f
                                    drawArc(
                                        color = categoryColors[category] ?: Color.Gray,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        topLeft = Offset(0f, 0f),
                                        size = Size(size.width, size.height),
                                        style = Stroke(width = 80f)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }
                    }
                }

                // Analisis (di dalam kotak putih dengan shadow)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Analisis",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        // Garis hitam di bawah "Analisis"
                        Divider(
                            color = Color.Black,
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        categoryData.entries.forEachIndexed { index, (category, data) ->
                            AnalysisItem(
                                category = category,
                                transactionCount = data.first,
                                totalAmount = data.second,
                                totalPengeluaran = totalPengeluaran,
                                navController = navController
                            )
//                            // Tambahkan garis pemisah kecuali untuk item terakhir
//                            if (index < categoryData.size - 1) {
//                                Spacer(modifier = Modifier.height(8.dp))
//                                Divider(
//                                    color = Color.Gray.copy(alpha = 0.2f),
//                                    thickness = 1.dp
//                                )
//                            }
                        }
                    }
                }
            }
        }
    }
}