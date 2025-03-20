package com.example.savvy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.savvy.data.AnimeEntity
import com.example.savvy.ui.home.HomeViewModel


@Composable
fun HomeScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: com.example.savvy.ui.home.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    // Collect the paging items from view Model.
    val animeList = viewModel.animeList.collectAsLazyPagingItems()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(items = List(animeList.itemCount) { it }) { index, _ ->
            animeList[index]?.let { anime ->
                NetflixAnimeItem(anime = anime, onClick = { onAnimeClick(anime.malId) })
            }
        }
    }
}

@Composable
fun NetflixAnimeItem(anime: AnimeEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(1f) // Ensures the card is square
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Display the anime image
            AsyncImage(
                model = anime.imageUrl,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay a gradient at the bottom for better text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
            )
            // Display the anime title on the gradient
            Text(
                text = anime.title,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}
