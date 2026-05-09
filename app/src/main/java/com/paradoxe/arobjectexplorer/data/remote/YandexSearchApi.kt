package com.paradoxe.arobjectexplorer.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface YandexSearchApi {
    @POST("search/v1/image")
    suspend fun searchByImage(
        @Header("Authorization") iamToken: String,
        @Header("x-folder-id") folderId: String,
        @Body request: YandexImageSearchRequest
    ): YandexImageSearchResponse
}

data class YandexImageSearchRequest(
    @SerializedName("image_content") val imageBase64: String
)

data class YandexImageSearchResponse(
    @SerializedName("results") val results: List<YandexResult>
)

data class YandexResult(
    @SerializedName("title") val title: String?,
    @SerializedName("snippet") val snippet: String?,
    @SerializedName("pageTitle") val pageTitle: String?
)
