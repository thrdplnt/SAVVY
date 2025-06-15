package com.example.savvy.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.AppRepository
import com.example.savvy.data.Transaction
import com.example.savvy.data.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Data class untuk menampung dompet beserta saldonya
data class WalletWithBalance(
    val wallet: Wallet,
    val balance: Long
)

// Data class untuk menampung seluruh state yang dibutuhkan oleh HomeScreen
data class HomeUiState(
    val walletsWithBalance: List<WalletWithBalance> = emptyList(),
    val monthlyCategoryExpenses: Map<String, Long> = emptyMap(),
    val monthlyCategoryCounts: Map<String, Int> = emptyMap(),
    val totalSaldo: Long = 0L,
    val totalPengeluaranBulanIni: Long = 0L,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<Transaction> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Flow sumber utama untuk semua transaksi
    private val allTransactionsFlow: Flow<List<Transaction>> = repository.transactions

    init {
        viewModelScope.launch {
            combine(
                repository.wallets, // Flow<List<Wallet>>
                repository.transactions
            ) { wallets, transactions ->
                Log.d("HomeViewModel", "Combining data. Wallets: ${wallets.size}, Transactions: ${transactions.size}")

                // 1. Hitung saldo untuk setiap dompet dengan filter yang diperbaiki
                val walletsWithBalance = wallets.map { wallet ->
                    val balance = transactions
                        .filter { trx ->
                            // KONDISI 1: Cocokkan berdasarkan ID dompet (ideal, untuk data baru)
                            trx.walletId == wallet.id ||
                                    // KONDISI 2 (FALLBACK): Jika walletId berisi NAMA dompet (untuk data lama)
                                    trx.walletId.equals(wallet.name, ignoreCase = true) ||
                                    // KONDISI 3 (FALLBACK TAMBAHAN): Jika walletId kosong, cocokkan berdasarkan NAMA dompet dari field `type`
                                    (trx.walletId.isNullOrBlank() && trx.type.equals(wallet.name, ignoreCase = true))
                        }
                        .sumOf { trx ->
                            if (trx.category == "Pemasukan") trx.amount else -trx.amount
                        }
                    Log.d("HomeViewModel", "Wallet: ${wallet.name}, ID: ${wallet.id}, Calculated Balance: $balance")
                    WalletWithBalance(wallet = wallet, balance = balance)
                }

                // 2. Hitung total saldo dari semua dompet yang terhitung
                val totalSaldo = walletsWithBalance.sumOf { it.balance }
                Log.d("HomeViewModel", "Calculated Total Saldo: $totalSaldo")

                // 3. Siapkan data untuk Analisis (pengeluaran bulan ini)
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                val monthlyExpenses = transactions.filter { trx ->
                    trx.category != "Pemasukan" && trx.date?.let { date ->
                        calendar.time = date
                        calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                    } ?: false
                }
                val categoryExpenses = monthlyExpenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
                val categoryCounts = monthlyExpenses.groupBy { it.category }.mapValues { entry -> entry.value.size }
                val totalPengeluaranBulanIni = categoryExpenses.values.sum()

                // 4. Update state UI
                // Menggunakan _uiState.update agar lebih aman dalam coroutine
                _uiState.update { currentState ->
                    currentState.copy(
                        walletsWithBalance = walletsWithBalance.sortedBy { it.wallet.name },
                        totalSaldo = totalSaldo,
                        monthlyCategoryExpenses = categoryExpenses,
                        monthlyCategoryCounts = categoryCounts,
                        totalPengeluaranBulanIni = totalPengeluaranBulanIni,
                        isLoading = false
                    )
                }

            }.catch { e ->
                Log.e("HomeViewModel", "Error in combine flow: $e")
                _uiState.update { it.copy(isLoading = false) }
            }.collect() // Cukup .collect() untuk menjalankan flow
        }
    }

    // Fungsi untuk mengubah query pencarian
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // Fungsi untuk menjalankan pencarian
    fun performSearch() {
        val query = _uiState.value.searchQuery.lowercase().trim()
        if (query.isBlank()) {
            clearSearch()
            return
        }
        viewModelScope.launch {
            val allTxs = allTransactionsFlow.first()
            val results = allTxs.filter { transaction ->
                val category = transaction.category.lowercase()
                val note = transaction.note.lowercase()
                val type = transaction.type.lowercase() // nama dompet
                val dateString = transaction.date?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale("id")).format(it)
                }?.lowercase() ?: ""
                category.contains(query) || note.contains(query) || type.contains(query) || dateString.contains(query)
            }
                .sortedByDescending { it.date } // <-- PERBAIKAN: Tambahkan baris ini untuk mengurutkan

            _uiState.update { it.copy(isSearching = true, searchResults = results) }
        }
    }

    // Fungsi untuk membersihkan/mengakhiri pencarian
    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList()
            )
        }
    }
}