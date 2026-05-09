package com.paradoxe.arobjectexplorer.data.repository

import com.paradoxe.arobjectexplorer.data.local.SearchResultDao
import com.paradoxe.arobjectexplorer.data.local.SearchResultEntity
import com.paradoxe.arobjectexplorer.data.remote.WikipediaApi
import com.paradoxe.arobjectexplorer.data.remote.YandexImageSearchRequest
import com.paradoxe.arobjectexplorer.data.remote.YandexSearchApi
import com.paradoxe.arobjectexplorer.domain.models.ScannedObject
import com.paradoxe.arobjectexplorer.domain.repository.ObjectRepository
import javax.inject.Inject

class ObjectRepositoryImpl @Inject constructor(
    private val yandexApi: YandexSearchApi,
    private val wikipediaApi: WikipediaApi,
    private val searchResultDao: SearchResultDao
) : ObjectRepository {

    override suspend fun identifyObject(
        imageBase64: String,
        imageHash: String,
        iamToken: String,
        folderId: String,
        lang: String
    ): Result<ScannedObject> {
        // 1. Check Cache
        val cached = searchResultDao.getResultByHash(imageHash)
        if (cached != null) {
            return Result.success(ScannedObject(
                id = cached.imageHash,
                name = cached.objectName,
                description = cached.description,
                thumbnailUrl = cached.thumbnailUrl,
                wikiUrl = cached.wikiUrl
            ))
        }

        return try {
            // 2. Yandex Image Search
            val yandexResponse = yandexApi.searchByImage(
                iamToken = "Bearer $iamToken",
                folderId = folderId,
                request = YandexImageSearchRequest(imageBase64)
            )

            val bestMatch = yandexResponse.results.firstOrNull()?.let {
                it.pageTitle ?: it.title ?: it.snippet
            } ?: return Result.failure(Exception("Object not recognized"))

            // 3. Wikipedia API
            val wikiSummary = wikipediaApi.getPageSummary(bestMatch)

            val scannedObject = ScannedObject(
                id = imageHash,
                name = wikiSummary.title,
                description = wikiSummary.extract,
                thumbnailUrl = wikiSummary.thumbnail?.source,
                wikiUrl = wikiSummary.contentUrls.desktop.page
            )

            // 4. Cache Result
            searchResultDao.insertResult(SearchResultEntity(
                imageHash = imageHash,
                objectName = scannedObject.name,
                description = scannedObject.description,
                thumbnailUrl = scannedObject.thumbnailUrl,
                wikiUrl = scannedObject.wikiUrl
            ))

            Result.success(scannedObject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
