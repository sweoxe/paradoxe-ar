package com.paradoxe.arobjectexplorer.data.repository

import com.paradoxe.arobjectexplorer.data.local.SearchResultDao
import com.paradoxe.arobjectexplorer.data.local.SearchResultEntity
import com.paradoxe.arobjectexplorer.data.remote.WikipediaApi
import com.paradoxe.arobjectexplorer.domain.models.ScannedObject
import com.paradoxe.arobjectexplorer.domain.repository.ObjectRepository
import javax.inject.Inject

class ObjectRepositoryImpl @Inject constructor(
    private val wikipediaApi: WikipediaApi,
    private val searchResultDao: SearchResultDao
) : ObjectRepository {

    override suspend fun getObjectInfo(
        label: String,
        lang: String
    ): Result<ScannedObject> {
        // 1. Check Cache
        val cached = searchResultDao.getResultByHash(label)
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
            // 2. Search Wikipedia title
            val searchResponse = wikipediaApi.searchPage(label)
            val pageTitle = searchResponse.query.search.firstOrNull()?.title 
                ?: return Result.failure(Exception("Not found on Wikipedia"))

            // 3. Get Wikipedia Summary
            val wikiSummary = wikipediaApi.getPageSummary(pageTitle)

            val scannedObject = ScannedObject(
                id = label, // Use label as ID/Hash for cache
                name = wikiSummary.title,
                description = wikiSummary.extract,
                thumbnailUrl = wikiSummary.thumbnail?.source,
                wikiUrl = wikiSummary.contentUrls.desktop.page
            )

            // 4. Cache Result
            searchResultDao.insertResult(SearchResultEntity(
                imageHash = label,
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
