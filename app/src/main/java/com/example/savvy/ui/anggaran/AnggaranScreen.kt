package com.example.savvy.ui.anggaran

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image // Import Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource // Import painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.savvy.R // Import R
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.Teal
import com.example.savvy.ui.theme.White
import com.example.savvy.ui.components.SavvyDropdownMenu
import com.example.savvy.ui.components.SavvyTextField
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnggaranScreen(
    navController: NavController,
    viewModel: AnggaranViewModel = hiltViewModel()
) {
    val anggaranItems by viewModel.anggaranUiItems.collectAsState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingAnggaran by remember { mutableStateOf<AnggaranUiItem?>(null) }

    var anggaranName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf(Date()) }

    val dateFormat = SimpleDateFormat("dd MMM yy", Locale("id"))
    val categories = listOf(
        "Makanan", "Transportasi", "Hiburan", "Pendidikan",
        "Tagihan", "Kesehatan", "Belanja", "Uang Keluar"
    )

    fun resetForm() {
        selectedCategory = categories.firstOrNull() ?: ""
        anggaranName = "Anggaran " + (categories.find { it == selectedCategory } ?: selectedCategory)
        amount = ""
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        startDate = cal.time
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = cal.time
    }

    fun openEditDialog(item: AnggaranUiItem) {
        editingAnggaran = item
        anggaranName = item.localAnggaran.name
        selectedCategory = item.localAnggaran.category
        amount = item.localAnggaran.amount.toString()
        startDate = item.localAnggaran.startDate
        endDate = item.localAnggaran.endDate
        showDialog = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Anggaran", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White // Latar belakang TopAppBar juga putih
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAnggaran = null
                    resetForm()
                    showDialog = true
                },
                containerColor = Navy,
                contentColor = White
            ) {
                Icon(Icons.Filled.Add, "Buat Anggaran")
            }
        },
        containerColor = White // Latar belakang utama Scaffold menjadi putih
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp), // Mengurangi padding vertikal sedikit
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (anggaranItems.isEmpty() && !viewModel.isLoading.value) { // Tambahkan cek !isLoading
                item {
                    Text(
                        text = "Belum ada anggaran yang dibuat.\nKetuk tombol '+' untuk memulai.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp)
                    )
                }
            } else if (viewModel.isLoading.value) {
                item {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 50.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Navy)
                    }
                }
            }
            items(anggaranItems) { item ->
                AnggaranCard(
                    item = item,
                    onEditClick = { openEditDialog(item) },
                    onDeleteClick = { viewModel.deleteAnggaran(item) }
                )
            }
            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }

    if (showDialog) {
        val startDialogCalendar = Calendar.getInstance().apply { time = startDate }
        val endDialogCalendar = Calendar.getInstance().apply { time = endDate }

        val startDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
                startDate = cal.time
                if (endDate.before(startDate)) {
                    val endCal = Calendar.getInstance().apply { time = startDate }
                    endCal.set(Calendar.HOUR_OF_DAY, 23)
                    endCal.set(Calendar.MINUTE, 59)
                    endCal.set(Calendar.SECOND, 59)
                    endDate = endCal.time
                }
            },
            startDialogCalendar.get(Calendar.YEAR),
            startDialogCalendar.get(Calendar.MONTH),
            startDialogCalendar.get(Calendar.DAY_OF_MONTH)
        )

        val endDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth, 23, 59, 59) }
                endDate = cal.time
            },
            endDialogCalendar.get(Calendar.YEAR),
            endDialogCalendar.get(Calendar.MONTH),
            endDialogCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = startDate.time // Pastikan minDate di set setelah startDate mungkin berubah
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingAnggaran == null) "Buat Anggaran Baru" else "Edit Anggaran", color = Navy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { // Sedikit menambah spasi
                    SavvyTextField(
                        value = anggaranName,
                        onValueChange = { anggaranName = it },
                        label = "Nama Anggaran" // Opsional dihapus agar lebih jelas
                    )
                    SavvyDropdownMenu(
                        label = "Kategori",
                        items = categories,
                        selectedItem = selectedCategory,
                        onItemSelected = {
                            selectedCategory = it
                            if (editingAnggaran == null && (anggaranName.startsWith("Anggaran ") || anggaranName.isBlank())) {
                                anggaranName = "Anggaran $it"
                            }
                        },
                        enabled = editingAnggaran == null
                    )
                    SavvyTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        label = "Jumlah Anggaran (Rp)",
                        keyboardType = KeyboardType.Number
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SavvyTextField(
                            value = dateFormat.format(startDate),
                            onValueChange = {},
                            label = "Tanggal Mulai",
                            readOnly = true,
                            onClickAction = { startDatePickerDialog.show() },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { startDatePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, "Pilih Tanggal Mulai", tint = Navy)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SavvyTextField(
                            value = dateFormat.format(endDate),
                            onValueChange = {},
                            label = "Tanggal Selesai",
                            readOnly = true,
                            onClickAction = { endDatePickerDialog.show() },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { endDatePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, "Pilih Tanggal Selesai", tint = Navy)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalAmount = amount.toLongOrNull()
                        if (selectedCategory.isNotBlank() && finalAmount != null && finalAmount > 0) {
                            val finalName = anggaranName.ifBlank { "Anggaran $selectedCategory" }
                            if (endDate.before(startDate)) {
                                Toast.makeText(context, "Tanggal selesai tidak boleh sebelum tanggal mulai.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (editingAnggaran == null) {
                                viewModel.addAnggaran(finalName, selectedCategory, finalAmount, startDate, endDate)
                            } else {
                                viewModel.updateAnggaran(editingAnggaran!!, finalAmount, startDate, endDate, finalName)
                            }
                            showDialog = false
                        } else {
                            Toast.makeText(context, "Harap isi semua field dengan benar.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy)
                ) { Text("Simpan", color = White) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Batal", color = Navy) }
            },
            containerColor = White
        )
    }
}

@Composable
fun AnggaranCard(item: AnggaranUiItem, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    currencyFormat.maximumFractionDigits = 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F0FA)), // Warna Card diubah
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon Kategori
            Box(
                modifier = Modifier
                    .size(48.dp) // Ukuran box untuk ikon
                    .clip(CircleShape)
                    .background(
                        when (item.localAnggaran.category) { // Warna background ikon
                            "Makanan" -> Color(0xFF6256D1).copy(alpha = 0.1f)
                            "Transportasi" -> Color(0xFF83E46F).copy(alpha = 0.1f)
                            "Hiburan" -> Color(0xFF4894FF).copy(alpha = 0.1f)
                            "Pendidikan" -> Color(0xFFFFD300).copy(alpha = 0.1f)
                            "Tagihan" -> Color(0xFFFF4A4A).copy(alpha = 0.1f)
                            "Kesehatan" -> Color(0xFF9DCFFF).copy(alpha = 0.1f)
                            "Belanja" -> Color(0xFFFF458A).copy(alpha = 0.1f)
                            "Uang Keluar" -> Color(0xFF76E7E7).copy(alpha = 0.1f)
                            else -> Color.Gray.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = when (item.localAnggaran.category) {
                            "Makanan" -> R.drawable.ic_makanan
                            "Transportasi" -> R.drawable.ic_transportasi
                            "Hiburan" -> R.drawable.ic_hiburan
                            "Pendidikan" -> R.drawable.ic_pendidikan
                            "Tagihan" -> R.drawable.ic_tagihan
                            "Kesehatan" -> R.drawable.ic_kesehatan
                            "Belanja" -> R.drawable.ic_belanja
                            "Uang Keluar" -> R.drawable.ic_uang_keluar
                            else -> R.drawable.ic_uang_keluar // Default icon
                        }
                    ),
                    contentDescription = item.localAnggaran.category,
                    modifier = Modifier.size(28.dp) // Ukuran ikon di dalam box
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.localAnggaran.name.ifBlank { "Anggaran ${item.localAnggaran.category}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text(
                    text = "Kategori: ${item.localAnggaran.category}",
                    style = MaterialTheme.typography.bodySmall, // Ukuran font lebih kecil
                    color = Navy.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = item.progres,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Teal,
                    trackColor = Navy.copy(alpha = 0.15f) // Lebih terang trackColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sisa: ${currencyFormat.format(item.sisa)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.sisa < 0) MaterialTheme.colorScheme.error else Navy.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(item.progres * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall, // Lebih kecil
                        fontWeight = FontWeight.Bold,
                        color = Teal
                    )
                }
                Text(
                    text = "dari ${currencyFormat.format(item.localAnggaran.amount)}",
                    style = MaterialTheme.typography.labelSmall, // Lebih kecil
                    color = Navy.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Berlaku: ${SimpleDateFormat("dd MMM", Locale("id")).format(item.localAnggaran.startDate)} - ${SimpleDateFormat("dd MMM yy", Locale("id")).format(item.localAnggaran.endDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Navy.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = Teal.copy(alpha = 0.8f))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}
