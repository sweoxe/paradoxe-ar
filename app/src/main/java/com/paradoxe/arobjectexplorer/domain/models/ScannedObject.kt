package com.paradoxe.arobjectexplorer.domain.models

data class ScannedObject(
    val id: String,
    val name: String,
    val description: String,
    val thumbnailUrl: String?,
    val wikiUrl: String
)
