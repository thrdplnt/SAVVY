package com.example.savvy.ui.anggaran

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvy.data.AppRepository
import com.example.savvy.data.LocalAnggaran
import com.example.savvy.data.LocalTransactionDao
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AnggaranUiItem(
    val localAnggaran: LocalAnggaran,
    val terpakai: Long,
    val sisa: Long,
    val progres: Float
)

@HiltViewModel
class AnggaranViewModel @Inject constructor(
    private val repository: AppRepository,
    private val localTransactionDao: LocalTransactionDao
) : ViewModel() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _isLoading = MutableStateFlow(true) // Status loading awal
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _anggaranUiItems = MutableStateFlow<List<AnggaranUiItem>>(emptyList())
    val anggaranUiItems: StateFlow<List<AnggaranUiItem>> = _anggaranUiItems.asStateFlow()

    init {
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                _isLoading.value = true // Mulai loading
                repository.anggaranList
                    .combine(localTransactionDao.getAllTransactions(userId)) { anggaranList, transactionList ->
                        anggaranList.map { anggaran ->
                            val transaksiTerkait = transactionList.filter { trx ->
                                trx.category == anggaran.category &&
                                        (trx.date?.let { it >= anggaran.startDate && it <= anggaran.endDate } ?: false) &&
                                        trx.category != "Pemasukan"
                            }
                            val terpakai = transaksiTerkait.sumOf { it.amount }
                            val sisa = anggaran.amount - terpakai
                            val progres = if (anggaran.amount > 0) (terpakai.toFloat() / anggaran.amount.toFloat()).coerceIn(0f, 1f) else 0f

                            AnggaranUiItem(
                                localAnggaran = anggaran,
                                terpakai = terpakai,
                                sisa = sisa,
                                progres = progres
                            )
                        }
                    }.catch { e ->
                        Log.e("AnggaranViewModel", "Error collecting anggaran items: $e")
                        _anggaranUiItems.value = emptyList()
                        _isLoading.value = false // Selesai loading (error)
                    }.collect { uiItems ->
                        _anggaranUiItems.value = uiItems.filter { it.localAnggaran.endDate >= Date() }
                            .sortedBy { it.localAnggaran.endDate }
                        _isLoading.value = false // Selesai loading (sukses)
                    }
            }
        } else {
            _isLoading.value = false // Tidak ada user, tidak loading
        }
    }

    suspend fun addAnggaran(
        name: String,
        category: String,
        amount: Long,
        startDate: Date,
        endDate: Date
    ): Boolean {
        if (userId.isEmpty()) return false

        val hasOverlap = _anggaranUiItems.value.any { item ->
            val existingAnggaran = item.localAnggaran
            existingAnggaran.category == category &&
                    startDate.time <= existingAnggaran.endDate.time &&
                    endDate.time >= existingAnggaran.startDate.time
        }

        if (hasOverlap) {
            Log.w("AnggaranViewModel", "Overlap detected for category: $category")
            return false
        }

        val newAnggaran = LocalAnggaran(
            userId = userId,
            name = name,
            category = category,
            amount = amount,
            startDate = startDate,
            endDate = endDate
        )
        repository.insertAnggaran(newAnggaran)
        return true
    }

    suspend fun updateAnggaran(
        anggaranUiItem: AnggaranUiItem,
        newAmount: Long,
        newStartDate: Date,
        newEndDate: Date,
        newName: String
    ): Boolean {
        val hasOverlap = _anggaranUiItems.value.any { item ->
            if (item.localAnggaran.clientGeneratedId == anggaranUiItem.localAnggaran.clientGeneratedId) {
                return@any false
            }
            val existingAnggaran = item.localAnggaran
            existingAnggaran.category == anggaranUiItem.localAnggaran.category &&
                    newStartDate.time <= existingAnggaran.endDate.time &&
                    newEndDate.time >= existingAnggaran.startDate.time
        }

        if (hasOverlap) {
            Log.w("AnggaranViewModel", "Overlap detected while updating for category: ${anggaranUiItem.localAnggaran.category}")
            return false
        }

        val updatedLocalAnggaran = anggaranUiItem.localAnggaran.copy(
            amount = newAmount,
            startDate = newStartDate,
            endDate = newEndDate,
            name = newName
        )
        repository.updateAnggaran(updatedLocalAnggaran)
        return true
    }


    fun deleteAnggaran(anggaranUiItem: AnggaranUiItem) {
        viewModelScope.launch {
            repository.deleteAnggaranByClientGeneratedId(anggaranUiItem.localAnggaran.clientGeneratedId)
        }
    }
}