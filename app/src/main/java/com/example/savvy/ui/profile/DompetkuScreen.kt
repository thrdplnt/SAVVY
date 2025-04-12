//package com.example.savvy.ui.profile
//
//import android.widget.Toast
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.example.savvy.data.AppRepository
//import com.example.savvy.data.Wallet
//import com.example.savvy.ui.components.SavvyTextField
//import com.example.savvy.ui.theme.Beige
//import com.example.savvy.ui.theme.Navy
//import com.example.savvy.ui.theme.White
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.launch
//
//@Composable
//fun DompetkuScreen(
//    navController: NavController,
//    repository: AppRepository
//) {
//    val context = LocalContext.current
//    val coroutineScope = rememberCoroutineScope()
//    val wallets by repository.wallets.collectAsState(initial = emptyList())
//
//    // State untuk dialog tambah/edit dompet
//    var showAddDialog by remember { mutableStateOf(false) }
//    var showEditDialog by remember { mutableStateOf(false) }
//    var showDeleteDialog by remember { mutableStateOf(false) }
//    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
//    var newWalletName by remember { mutableStateOf("") }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Beige)
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = "Dompetku",
//            fontSize = 24.sp,
//            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
//            color = Navy,
//            modifier = Modifier.padding(bottom = 16.dp)
//        )
//
//        // Tombol Tambah Dompet
//        Button(
//            onClick = {
//                newWalletName = ""
//                showAddDialog = true
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp),
//            shape = RoundedCornerShape(8.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = Navy,
//                contentColor = White
//            )
//        ) {
//            Icon(
//                imageVector = Icons.Default.Add,
//                contentDescription = "Tambah Dompet",
//                modifier = Modifier.size(24.dp)
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = "Tambah Dompet",
//                fontSize = 16.sp
//            )
//        }
//
//        // Daftar Dompet
//        if (wallets.isEmpty()) {
//            Text(
//                text = "Belum ada dompet",
//                style = MaterialTheme.typography.bodyLarge,
//                color = Navy,
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//        } else {
//            LazyColumn(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(wallets) { wallet ->
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = White
//                        ),
//                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                text = wallet.name,
//                                style = MaterialTheme.typography.bodyLarge,
//                                color = Navy
//                            )
//                            Row {
//                                Icon(
//                                    imageVector = Icons.Default.Edit,
//                                    contentDescription = "Edit Dompet",
//                                    tint = Navy,
//                                    modifier = Modifier
//                                        .clickable {
//                                            selectedWallet = wallet
//                                            newWalletName = wallet.name
//                                            showEditDialog = true
//                                        }
//                                        .padding(end = 16.dp)
//                                )
//                                Icon(
//                                    imageVector = Icons.Default.Delete,
//                                    contentDescription = "Hapus Dompet",
//                                    tint = Navy,
//                                    modifier = Modifier
//                                        .clickable {
//                                            selectedWallet = wallet
//                                            showDeleteDialog = true
//                                        }
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // Dialog Tambah Dompet
//    if (showAddDialog) {
//        AlertDialog(
//            onDismissRequest = { showAddDialog = false },
//            title = {
//                Text(
//                    text = "Tambah Dompet",
//                    style = MaterialTheme.typography.headlineMedium,
//                    color = Navy
//                )
//            },
//            text = {
//                SavvyTextField(
//                    value = newWalletName,
//                    onValueChange = { newWalletName = it },
//                    label = "Nama Dompet",
//                    modifier = Modifier.fillMaxWidth()
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        if (newWalletName.isBlank()) {
//                            Toast.makeText(context, "Nama dompet tidak boleh kosong", Toast.LENGTH_SHORT).show()
//                            return@TextButton
//                        }
//                        coroutineScope.launch {
//                            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
//                            repository.insertWallet(Wallet(name = newWalletName, userId = userId))
//                            showAddDialog = false
//                        }
//                    }
//                ) {
//                    Text(
//                        text = "Simpan",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Navy
//                    )
//                }
//            },
//            dismissButton = {
//                TextButton(
//                    onClick = { showAddDialog = false }
//                ) {
//                    Text(
//                        text = "Batal",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Navy
//                    )
//                }
//            },
//            containerColor = White
//        )
//    }
//
//    // Dialog Edit Dompet
//    if (showEditDialog && selectedWallet != null) {
//        AlertDialog(
//            onDismissRequest = { showEditDialog = false },
//            title = {
//                Text(
//                    text = "Edit Dompet",
//                    style = MaterialTheme.typography.headlineMedium,
//                    color = Navy
//                )
//            },
//            text = {
//                SavvyTextField(
//                    value = newWalletName,
//                    onValueChange = { newWalletName = it },
//                    label = "Nama Dompet",
//                    modifier = Modifier.fillMaxWidth()
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        if (newWalletName.isBlank()) {
//                            Toast.makeText(context, "Nama dompet tidak boleh kosong", Toast.LENGTH_SHORT).show()
//                            return@TextButton
//                        }
//                        coroutineScope.launch {
//                            selectedWallet?.let {
//                                repository.updateWallet(it.copy(name = newWalletName))
//                                showEditDialog = false
//                            }
//                        }
//                    }
//                ) {
//                    Text(
//                        text = "Simpan",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Navy
//                    )
//                }
//            },
//            dismissButton = {
//                TextButton(
//                    onClick = { showEditDialog = false }
//                ) {
//                    Text(
//                        text = "Batal",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Navy
//                    )
//                }
//            },
//            containerColor = White
//        )
//    }
//
//    // Dialog Konfirmasi Hapus Dompet
//    if (showDeleteDialog && selectedWallet != null) {
//        AlertDialog(
//            onDismissRequest = { showDeleteDialog = false },
//            title = {
//                Text(
//                    text = "Hapus Dompet",
//                    style = MaterialTheme.typography.headlineMedium,
//                    color = Navy
//                )
//            },
//            text = {
//                Text(
//                    text = "Apakah Anda yakin ingin menghapus dompet ${selectedWallet?.name}?",
//                    style = MaterialTheme.typography.bodyLarge,
//                    color = Navy
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        coroutineScope.launch {
//                            selectedWallet?.let {
//                                repository.deleteWallet(it.id)
//                                showDeleteDialog = false
//                            }
//                        }
//                    }
//                ) {
//                    Text(
//                        text = "Hapus",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Navy
//                    )
//                }
//            },
//            dismissButton = {
//                TextButton(
//                    onClick = { showDeleteDialog = false }
//                ) {
//                    Text(
//                        text = "Batal",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Navy
//                    )
//                }
//            },
//            containerColor = White
//        )
//    }
//}