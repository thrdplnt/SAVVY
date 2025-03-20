package com.example.savvy.data

import androidx.paging.PagingSource
import androidx.paging.PagingState

class SearchAnimePagingSource(
    private val apiService: JikanApiService,
    private val query: String
) : PagingSource<Int, AnimeNetworkModel>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AnimeNetworkModel> {
        val page = params.key ?: 1
        return try {
            val response = apiService.searchAnime(query, page)
            LoadResult.Page(
                data = response.data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.data.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, AnimeNetworkModel>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}
