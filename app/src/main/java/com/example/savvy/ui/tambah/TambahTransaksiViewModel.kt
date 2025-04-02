package com.example.savvy.ui.tambah

import androidx.lifecycle.ViewModel
import com.example.savvy.data.SupabaseStorageUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TambahTransaksiViewModel @Inject constructor(
    val uploader: SupabaseStorageUploader
) : ViewModel()