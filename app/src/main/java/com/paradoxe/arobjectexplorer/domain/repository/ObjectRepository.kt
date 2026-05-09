package com.paradoxe.arobjectexplorer.domain.repository

import com.paradoxe.arobjectexplorer.domain.models.ScannedObject
import kotlinx.coroutines.flow.Flow

interface ObjectRepository {
    suspend fun identifyObject(
        imageBase64: String,
        imageHash: String,
        iamToken: String,
        folderId: String,
        lang: String = "ru"
    ): Result<ScannedObject>
}
