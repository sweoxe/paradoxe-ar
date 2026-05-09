package com.paradoxe.arobjectexplorer.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface WikipediaApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String
    ): WikipediaSummary
}

data class WikipediaSummary(
    @SerializedName("title") val title: String,
    @SerializedName("extract") val extract: String,
    @SerializedName("thumbnail") val thumbnail: Thumbnail?,
    @SerializedName("content_urls") val contentUrls: ContentUrls
)

data class Thumbnail(
    @SerializedName("source") val source: String
)

data class ContentUrls(
    @SerializedName("desktop") val desktop: DesktopUrls
)

data class DesktopUrls(
    @SerializedName("page") val page: String
)
