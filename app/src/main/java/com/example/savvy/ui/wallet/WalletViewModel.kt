package com.example.savvy.ui.wallet // Buat package ini jika belum ada

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.AppRepository
import com.example.savvy.data.Wallet
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class baru untuk UI, menggabungkan dompet dengan saldonya yang sudah dihitung
data class WalletUiItem(
    val wallet: Wallet,
    val balance: Long
)

data class WalletUiState(
    val walletItems: List<WalletUiItem> = emptyList(), // Mengganti List<Wallet>
    val isLoading: Boolean = true, // Mulai dengan loading
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDialog: Boolean = false,
    val walletToEdit: Wallet? = null,
    val newWalletName: String = ""
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadWalletsWithBalance()
    }

    private fun loadWalletsWithBalance() {
        userId?.let {
            viewModelScope.launch {
                // Gabungkan flow wallets dan transactions untuk perhitungan
                appRepository.wallets.combine(appRepository.transactions) { wallets, transactions ->
                    Log.d("WalletViewModel", "Combining data. Wallets: ${wallets.size}, Transactions: ${transactions.size}")

                    // Lakukan perhitungan saldo untuk setiap dompet
                    wallets.map { wallet ->
                        val balance = transactions
                            .filter { trx ->
                                // Logika untuk mencocokkan transaksi dengan dompet (termasuk data lama)
                                trx.walletId == wallet.id || (trx.walletId.isNullOrBlank() && trx.type.equals(wallet.name, ignoreCase = true))
                            }
                            .sumOf { trx ->
                                if (trx.category == "Pemasukan") trx.amount else -trx.amount
                            }
                        WalletUiItem(wallet = wallet, balance = balance)
                    }
                }
                    .onStart { _uiState.update { it.copy(isLoading = true) } }
                    .catch { e ->
                        Log.e("WalletViewModel", "Error combining flows: $e")
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Gagal memuat data dompet.") }
                    }
                    .collect { calculatedWalletItems ->
                        _uiState.update { currentState ->
                            // PERBAIKAN: Listener ini hanya bertanggung jawab untuk memperbarui data
                            // dan status loading awal, tidak mengganggu state dialog.
                            currentState.copy(
                                isLoading = if(currentState.isLoading) false else currentState.isLoading, // Hanya set false sekali saat awal
                                walletItems = calculatedWalletItems.sortedBy { item -> item.wallet.name }
                            )
                        }
                    }
            }
        } ?: _uiState.update { it.copy(isLoading = false, errorMessage = "Pengguna tidak login.") }
    }


    fun onDialogDismiss() {
        _uiState.update { it.copy(showDialog = false, walletToEdit = null, newWalletName = "", errorMessage = null, successMessage = null) }
    }

    fun onOpenAddDialog() {
        _uiState.update { it.copy(showDialog = true, walletToEdit = null, newWalletName = "") }
    }

    fun onOpenEditDialog(walletUiItem: WalletUiItem) {
        _uiState.update { it.copy(showDialog = true, walletToEdit = walletUiItem.wallet, newWalletName = walletUiItem.wallet.name) }
    }

    fun onWalletNameChange(name: String) {
        _uiState.update { it.copy(newWalletName = name) }
    }

    fun clearUserMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun saveWallet() {
        val currentUserId = userId
        val nameToSave = uiState.value.newWalletName.trim()
        val editingWallet = uiState.value.walletToEdit

        if (currentUserId == null) { _uiState.update { it.copy(errorMessage = "Sesi berakhir.") }; return }
        if (nameToSave.isBlank()) { _uiState.update { it.copy(errorMessage = "Nama dompet tidak boleh kosong.") }; return }
        if (uiState.value.walletItems.any { it.wallet.name.equals(nameToSave, ignoreCase = true) && it.wallet.id != editingWallet?.id }) {
            _uiState.update { it.copy(errorMessage = "Nama dompet '$nameToSave' sudah ada.") }; return
        }

        viewModelScope.launch {
            // Tampilkan loading yang spesifik untuk aksi ini
            _uiState.update { it.copy(isLoading = true) }

            val result: Result<Unit> = if (editingWallet == null) {
                appRepository.addWallet(nameToSave, currentUserId)
            } else {
                if (editingWallet.name.equals(nameToSave, ignoreCase = true)) {
                    _uiState.update { it.copy(isLoading = false, showDialog = false) } // Langsung tutup jika tidak ada perubahan
                    return@launch
                }
                appRepository.updateWalletName(editingWallet.id, nameToSave, currentUserId)
            }

            // --- PERBAIKAN UTAMA ---
            // Blok ini sekarang menjadi satu-satunya yang bertanggung jawab untuk
            // mengubah state setelah operasi simpan selesai.
            result.fold(
                onSuccess = {
                    val successMsg = if (editingWallet == null) "Dompet '$nameToSave' berhasil ditambahkan." else "Dompet berhasil diubah."
                    // Update semua state yang relevan dalam satu panggilan update
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showDialog = false, // <-- Menutup dialog secara eksplisit
                            successMessage = successMsg,
                            walletToEdit = null, // Reset form
                            newWalletName = ""
                        )
                    }
                },
                onFailure = { e ->
                    // Jika gagal, hentikan loading dan tampilkan pesan error, dialog tetap terbuka
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Gagal menyimpan dompet."
                        )
                    }
                }
            )
        }
    }

    fun deleteWallet(walletUiItem: WalletUiItem) {
        val currentUserId = userId ?: return
        val wallet = walletUiItem.wallet

        // Logika pengecekan dipusatkan di ViewModel
        val defaultWallets = listOf("Tunai", "Tabungan", "Non-Tunai")
        if (defaultWallets.any { it.equals(wallet.name, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "Dompet default ('${wallet.name}') tidak bisa dihapus.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val hasTransactions = appRepository.hasTransactionsForWallet(wallet.id, currentUserId)
            if (hasTransactions) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Dompet '${wallet.name}' tidak bisa dihapus karena masih memiliki transaksi.") }
                return@launch
            }

            val result = appRepository.deleteWallet(wallet.id, currentUserId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, successMessage = "Dompet '${wallet.name}' berhasil dihapus.") } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Gagal menghapus dompet.") } }
            )
        }
    }
}