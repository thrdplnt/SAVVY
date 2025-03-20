package com.example.savvy.ui.search

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.example.savvy.data.AnimeEntity
import com.example.savvy.data.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")

    // Without cachedIn; note that this might trigger new loads on each collection
    val searchResults: Flow<PagingData<AnimeEntity>> = _query
        .debounce(500)
        .filter { it.isNotBlank() }
        .flatMapLatest { repository.searchAnime(it) }
    // Optionally use cachedIn if needed
    //.cachedIn(viewModelScope)

    fun searchAnime(query: String) {
        _query.value = query
    }
}
