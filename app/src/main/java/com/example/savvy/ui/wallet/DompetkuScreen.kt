package com.example.savvy.ui.wallet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.savvy.data.Wallet
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.theme.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DompetkuScreen(
    navController: NavController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirmationDialog by remember { mutableStateOf<Wallet?>(null) } // Menyimpan objek Wallet yang akan dihapus

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUserMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dompetku", fontWeight = FontWeight.Bold, color = Navy) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Navy)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White,
                    titleContentColor = Navy,
                    navigationIconContentColor = Navy
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onOpenAddDialog() },
                containerColor = Navy,
                contentColor = White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, "Tambah Dompet Baru")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (uiState.isLoading && uiState.wallets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Navy)
                }
            } else if (uiState.wallets.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 50.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada dompet.\nKetuk tombol (+) untuk menambah.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Navy.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.wallets, key = { it.id }) { wallet ->
                        WalletItemCard(
                            wallet = wallet,
                            onEditClick = { viewModel.onOpenEditDialog(wallet) },
                            onDeleteClick = {
                                val defaultWallets = listOf("Tunai", "Tabungan", "Non-Tunai")
                                if (defaultWallets.any { it.equals(wallet.name, ignoreCase = true) }) {
                                    Toast.makeText(context, "Dompet default ('${wallet.name}') tidak bisa dihapus.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDeleteConfirmationDialog = wallet
                                }
                            }
                        )
                    }
                }
            }

            if (uiState.showDialog) {
                AddEditWalletDialog(
                    walletToEdit = uiState.walletToEdit,
                    currentName = uiState.newWalletName,
                    onNameChange = { viewModel.onWalletNameChange(it) },
                    onDismiss = { viewModel.onDialogDismiss() },
                    onConfirm = { viewModel.saveWallet() },
                    isLoading = uiState.isLoading
                )
            }

            showDeleteConfirmationDialog?.let { walletToDelete ->
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmationDialog = null },
                    title = { Text("Konfirmasi Hapus", color = Navy) },
                    text = { Text("Apakah Anda yakin ingin menghapus dompet '${walletToDelete.name}'?", color = Navy.copy(alpha = 0.8f)) }, // Pesan lebih singkat
                    confirmButton = {
                        Button(
                            onClick = {
                                // --- PERBAIKAN DI SINI ---
                                viewModel.deleteWallet(walletToDelete) // Kirim seluruh objek Wallet
                                showDeleteConfirmationDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) { Text("Hapus", color = White) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmationDialog = null }) {
                            Text("Batal", color = Navy)
                        }
                    },
                    containerColor = White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

// Composable WalletItemCard dan AddEditWalletDialog tetap sama seperti respons sebelumnya
@Composable
fun WalletItemCard(
    wallet: Wallet,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    val isDefaultWallet = listOf("Tunai", "Tabungan", "Non-Tunai").any { it.equals(wallet.name, ignoreCase = true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Beige),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = "Ikon Dompet",
                tint = Navy,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Navy
                )
                Text(
                    text = "Saldo: ${currencyFormat.format(wallet.balance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Navy.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Dompet", tint = Teal)
                }
                if (!isDefaultWallet) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus Dompet", tint = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditWalletDialog(
    walletToEdit: Wallet?,
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(if (walletToEdit == null) "Tambah Dompet Baru" else "Edit Nama Dompet", color = Navy, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                SavvyTextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    label = "Nama Dompet",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentName.isNotBlank()) {
                        onConfirm()
                    } else {
                        Toast.makeText(context, "Nama dompet tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Text("Simpan", color = White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Batal", color = Navy)
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(16.dp)
    )
}