package com.example.savvy.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.savvy.ui.search.SearchViewModel
import com.example.savvy.ui.anime.AnimeItem

@Composable
fun SearchScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val results = viewModel.searchResults.collectAsLazyPagingItems()

    Column {
        TextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.searchAnime(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Search anime...") }
        )
        LazyColumn {
            items(results.itemCount) { index ->
                results[index]?.let { anime ->
                    AnimeItem(anime, onClick = { onAnimeClick(anime.malId) })
                }
            }
        }
    }
}
