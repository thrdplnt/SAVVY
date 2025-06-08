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

data class WalletUiState(
    val wallets: List<Wallet> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDialog: Boolean = false, // Untuk mengontrol visibilitas dialog tambah/edit
    val walletToEdit: Wallet? = null, // Dompet yang sedang diedit, null jika mode tambah
    val newWalletName: String = "" // Nama dompet untuk form di dialog
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
        loadWallets()
    }

    private fun loadWallets() {
        userId?.let { currentUid ->
            viewModelScope.launch {
                appRepository.wallets // Ini adalah Flow<List<Wallet>> dari AppRepository
                    .onStart {
                        Log.d("WalletViewModel", "Mulai mengambil daftar dompet...")
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    .catch { e ->
                        Log.e("WalletViewModel", "Error mengambil daftar dompet: $e")
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Gagal memuat dompet: ${e.message}") }
                    }
                    .collect { walletList ->
                        Log.d("WalletViewModel", "Daftar dompet diterima: ${walletList.size} item")
                        _uiState.update { it.copy(isLoading = false, wallets = walletList) }
                    }
            }
        } ?: run {
            Log.w("WalletViewModel", "Tidak bisa memuat dompet: pengguna tidak login.")
            _uiState.update { it.copy(isLoading = false, errorMessage = "Pengguna tidak login.") }
        }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(showDialog = false, walletToEdit = null, newWalletName = "", errorMessage = null, successMessage = null) }
    }

    fun onOpenAddDialog() {
        _uiState.update { it.copy(showDialog = true, walletToEdit = null, newWalletName = "") }
    }

    fun onOpenEditDialog(wallet: Wallet) {
        _uiState.update { it.copy(showDialog = true, walletToEdit = wallet, newWalletName = wallet.name) }
    }

    fun onWalletNameChange(name: String) {
        _uiState.update { it.copy(newWalletName = name) }
    }

    fun clearUserMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun saveWallet() {
        val currentUserId = userId
        val nameToSave = uiState.value.newWalletName.trim() // Hilangkan spasi di awal/akhir
        val editingWallet = uiState.value.walletToEdit

        if (currentUserId == null) {
            _uiState.update { it.copy(errorMessage = "Sesi berakhir, harap login ulang.", isLoading = false) }
            return
        }
        if (nameToSave.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama dompet tidak boleh kosong.", isLoading = false) }
            return
        }
        // Cek duplikasi nama (kecuali jika mengedit dan namanya tidak berubah dari nama aslinya)
        if (uiState.value.wallets.any { it.name.equals(nameToSave, ignoreCase = true) && it.id != editingWallet?.id }) {
            _uiState.update { it.copy(errorMessage = "Nama dompet '$nameToSave' sudah ada.", isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result: Result<Unit> = if (editingWallet == null) { // Mode Tambah
                appRepository.addWallet(nameToSave, currentUserId)
            } else { // Mode Edit
                if (editingWallet.name.equals(nameToSave, ignoreCase = true)) { // Nama tidak berubah
                    _uiState.update { it.copy(isLoading = false, showDialog = false, successMessage = "Tidak ada perubahan pada nama dompet.") }
                    return@launch
                }
                appRepository.updateWalletName(editingWallet.id, nameToSave, currentUserId)
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showDialog = false, // Tutup dialog setelah sukses
                            successMessage = if (editingWallet == null) "Dompet '$nameToSave' berhasil ditambahkan." else "Dompet berhasil diubah menjadi '$nameToSave'."
                        )
                    }
                    // loadWallets() // Muat ulang daftar dompet (Flow seharusnya otomatis update dari AppRepository)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // showDialog tetap true agar user bisa koreksi atau lihat error
                            errorMessage = e.message ?: "Terjadi kesalahan saat menyimpan dompet."
                        )
                    }
                }
            )
        }
    }

    fun deleteWallet(wallet: Wallet) { // Menerima objek Wallet agar bisa cek nama
        val currentUserId = userId
        if (currentUserId == null) {
            _uiState.update { it.copy(errorMessage = "Sesi berakhir, harap login ulang.") }
            return
        }

        // Cek apakah dompet default
        val defaultWallets = listOf("Tunai", "Tabungan", "Non-Tunai")
        if (defaultWallets.any { it.equals(wallet.name, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "Dompet default ('${wallet.name}') tidak bisa dihapus.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            // Cek dulu apakah ada transaksi terkait
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