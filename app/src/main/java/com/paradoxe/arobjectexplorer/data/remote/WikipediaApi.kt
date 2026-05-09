package com.paradoxe.arobjectexplorer.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface WikipediaApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String
    ): WikipediaSummary

    @GET("w/api.php?action=query&list=search&format=json&srlimit=1")
    suspend fun searchPage(
        @retrofit2.http.Query("srsearch") query: String
    ): WikipediaSearchResponse
}

data class WikipediaSearchResponse(
    @SerializedName("query") val query: WikipediaQuery
)

data class WikipediaQuery(
    @SerializedName("search") val search: List<WikipediaSearchResult>
)

data class WikipediaSearchResult(
    @SerializedName("title") val title: String
)

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
