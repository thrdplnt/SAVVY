package com.example.savvy.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class AnimeRepository @Inject constructor(
    private val apiService: JikanApiService,
    private val animeDao: AnimeDao,
    private val profileDao: ProfileDao
) {
    fun getTopAnime(): Flow<PagingData<AnimeEntity>> {
        Log.d("AnimeRepository", "Refreshing anime")
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { TopAnimePagingSource(apiService) }
        ).flow.map { pagingData ->
            pagingData.map { net ->
                val entity = AnimeEntity(
                    malId = net.malId,
                    title = net.title,
                    imageUrl = net.images.jpg.imageUrl ?: "https://example.com/placeholder.jpg"
                )
                animeDao.insert(entity)
                entity
            }
        }
    }

    fun getBookmarks(): Flow<List<AnimeEntity>> = animeDao.getBookmarkedAnime()

    suspend fun setBookmark(animeId: Int, isBookmarked: Boolean) {
        val anime = animeDao.getAnimeById(animeId) ?: return
        animeDao.update(anime.copy(isBookmarked = isBookmarked))
    }

    fun searchAnime(query: String): Flow<PagingData<AnimeEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { SearchAnimePagingSource(apiService, query) }
        ).flow.map { pagingData ->
            pagingData.map { net ->
                AnimeEntity(
                    malId = net.malId,
                    title = net.title,
                    imageUrl = net.images.jpg.imageUrl ?: "https://example.com/placeholder.jpg"
                )
            }
        }
    }


    suspend fun saveProfileImagePath(path: String) {
        profileDao.insert(ProfileEntity(imagePath = path))
    }

    suspend fun getProfileImagePath(): String? {
        return profileDao.getProfile()?.imagePath
    }
}
