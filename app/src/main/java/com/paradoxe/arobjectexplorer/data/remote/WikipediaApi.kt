package com.paradoxe.arobjectexplorer.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface WikipediaApi {
    @GET
    suspend fun getPageSummary(
        @Url url: String
    ): WikipediaSummary

    @GET
    suspend fun searchPage(
        @Url url: String
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
