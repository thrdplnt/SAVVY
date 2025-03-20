package com.example.savvy.data

import com.google.gson.annotations.SerializedName

data class AnimeSearchResponse(
    @SerializedName("data") val data: List<AnimeNetworkModel>
)

data class AnimeNetworkModel(
    @SerializedName("mal_id") val malId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("images") val images: AnimeImages
)

data class AnimeImages(
    @SerializedName("jpg") val jpg: AnimeJpg
)

data class AnimeJpg(
    @SerializedName("image_url") val imageUrl: String?
)
