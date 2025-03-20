package com.example.savvy.ui.bookmark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.savvy.ui.bookmark.BookmarkViewModel

@Composable
fun BookmarkScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: BookmarkViewModel = hiltViewModel()
) {
    val bookmarked = viewModel.bookmarkedAnime.collectAsState(initial = emptyList()).value

    Column {
        bookmarked.forEach { anime ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnimeClick(anime.malId) }
                    .padding(8.dp)
            ) {
                Text(anime.title)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleBookmark(anime.malId, anime.isBookmarked) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}
