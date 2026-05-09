package com.paradoxe.arobjectexplorer.domain.repository

import com.paradoxe.arobjectexplorer.domain.models.ScannedObject
import kotlinx.coroutines.flow.Flow

interface ObjectRepository {
    suspend fun getObjectInfo(
        label: String,
        lang: String = "ru"
    ): Result<ScannedObject>
}
