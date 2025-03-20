package com.example.savvy.data

import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApiService {
    @GET("top/anime")
    suspend fun getTopAnime(@Query("page") page: Int): AnimeSearchResponse

    @GET("anime")
    suspend fun searchAnime(@Query("q") query: String, @Query("page") page: Int): AnimeSearchResponse
}
