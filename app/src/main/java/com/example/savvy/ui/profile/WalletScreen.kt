//package com.example.savvy.ui.profile
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavController
//import com.example.savvy.data.Wallet
//import com.example.savvy.ui.theme.Beige
//import com.example.savvy.ui.theme.Navy
//import com.example.savvy.ui.theme.White
//import java.text.NumberFormat
//import java.util.*
//
//@Composable
//fun WalletScreen(
//    navController: NavController,
//    viewModel: WalletViewModel = hiltViewModel()
//) {
//    val wallets by viewModel.wallets.collectAsState()
//    var showAddDialog by remember { mutableStateOf(false) }
//    var showEditDialog by remember { mutableStateOf(false) }
//    var showDeleteDialog by remember { mutableStateOf(false) }
//    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
//    var newWalletName by remember { mutableStateOf("") }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(White)
//            .padding(16.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = { navController.popBackStack() }) {
//                Icon(
//                    imageVector = Icons.Default.ArrowBack,
//                    contentDescription = "Kembali",
//                    tint = Navy
//                )
//            }
//            Text(
//                text = "Dompetku",
//                style = MaterialTheme.typography.headlineLarge,
//                color = Navy,
//                modifier = Modifier.weight(1f)
//            )
//            IconButton(onClick = { showAddDialog = true }) {
//                Icon(
//                    imageVector = Icons.Default.Add,
//                    contentDescription = "Tambah Dompet",
//                    tint = Navy
//                )
//            }
//        }
//
//        LazyColumn {
//            items(wallets) { wallet ->
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 4.dp),
//                    shape = RoundedCornerShape(8.dp),
//                    colors = CardDefaults.cardColors(containerColor = Beige),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Column {
//                            Text(
//                                text = wallet.name,
//                                style = MaterialTheme.typography.bodyLarge,
//                                color = Navy
//                            )
//                            Text(
//                                text = "Rp ${NumberFormat.getNumberInstance(Locale("id")).format(wallet.balance)}",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = Navy
//                            )
//                        }
//                        Row {
//                            if (wallet.name !in listOf("Tunai", "Tabungan")) {
//                                IconButton(onClick = {
//                                    selectedWallet = wallet
//                                    newWalletName = wallet.name
//                                    showEditDialog = true
//                                }) {
//                                    Icon(
//                                        imageVector = Icons.Default.Edit,
//                                        contentDescription = "Edit",
//                                        tint = Navy
//                                    )
//                                }
//                                IconButton(onClick = {
//                                    selectedWallet = wallet
//                                    showDeleteDialog = true
//                                }) {
//                                    Icon(
//                                        imageVector = Icons.Default.Delete,
//                                        contentDescription = "Hapus",
//                                        tint = Navy
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // Dialog untuk menambah dompet
//    if (showAddDialog) {
//        AlertDialog(
//            onDismissRequest = { showAddDialog = false },
//            title = { Text("Tambah Dompet", color = Navy) },
//            text = {
//                OutlinedTextField(
//                    value = newWalletName,
//                    onValueChange = { newWalletName = it },
//                    label = { Text("Nama Dompet") },
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = Navy,
//                        unfocusedBorderColor = Navy
//                    )
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        if (newWalletName.isNotBlank() && newWalletName !in wallets.map { it.name }) {
//                            viewModel.addWallet(newWalletName)
//                            newWalletName = ""
//                            showAddDialog = false
//                        }
//                    },
//                    enabled = newWalletName.isNotBlank() && newWalletName !in wallets.map { it.name }
//                ) {
//                    Text("Tambah", color = Navy)
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showAddDialog = false }) {
//                    Text("Batal", color = Navy)
//                }
//            },
//            containerColor = White
//        )
//    }
//
//    // Dialog untuk mengedit dompet
//    if (showEditDialog && selectedWallet != null) {
//        AlertDialog(
//            onDismissRequest = { showEditDialog = false },
//            title = { Text("Edit Dompet", color = Navy) },
//            text = {
//                OutlinedTextField(
//                    value = newWalletName,
//                    onValueChange = { newWalletName = it },
//                    label = { Text("Nama Dompet") },
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = Navy,
//                        unfocusedBorderColor = Navy
//                    )
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        if (newWalletName.isNotBlank() && newWalletName !in wallets.map { it.name }) {
//                            selectedWallet?.let { wallet ->
//                                viewModel.updateWallet(wallet.copy(name = newWalletName))
//                            }
//                            showEditDialog = false
//                        }
//                    },
//                    enabled = newWalletName.isNotBlank() && newWalletName !in wallets.map { it.name }
//                ) {
//                    Text("Simpan", color = Navy)
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showEditDialog = false }) {
//                    Text("Batal", color = Navy)
//                }
//            },
//            containerColor = White
//        )
//    }
//
//    // Dialog untuk menghapus dompet
//    if (showDeleteDialog && selectedWallet != null) {
//        AlertDialog(
//            onDismissRequest = { showDeleteDialog = false },
//            title = { Text("Hapus Dompet", color = Navy) },
//            text = { Text("Apakah Anda yakin ingin menghapus dompet '${selectedWallet?.name}'?") },
//            confirmButton = {
//                TextButton(onClick = {
//                    selectedWallet?.let { wallet ->
//                        viewModel.deleteWallet(wallet.id)
//                    }
//                    showDeleteDialog = false
//                }) {
//                    Text("Hapus", color = MaterialTheme.colorScheme.error)
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showDeleteDialog = false }) {
//                    Text("Batal", color = Navy)
//                }
//            },
//            containerColor = White
//        )
//    }
//}