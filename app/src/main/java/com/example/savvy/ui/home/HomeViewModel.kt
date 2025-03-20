package com.example.savvy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.savvy.data.AnimeEntity
import com.example.savvy.data.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {
    val animeList: Flow<PagingData<AnimeEntity>> = repository.getTopAnime()
        .cachedIn(viewModelScope)
}
