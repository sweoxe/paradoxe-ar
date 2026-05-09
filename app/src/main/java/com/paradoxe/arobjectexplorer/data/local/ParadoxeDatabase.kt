package com.paradoxe.arobjectexplorer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "search_results")
data class SearchResultEntity(
    @PrimaryKey val imageHash: String,
    val objectName: String,
    val description: String,
    val thumbnailUrl: String?,
    val wikiUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SearchResultDao {
    @Query("SELECT * FROM search_results WHERE imageHash = :hash")
    suspend fun getResultByHash(hash: String): SearchResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: SearchResultEntity)

    @Query("DELETE FROM search_results WHERE timestamp < :expiryTime")
    suspend fun clearOldResults(expiryTime: Long)
}

@Database(entities = [SearchResultEntity::class], version = 1)
abstract class ParadoxeDatabase : RoomDatabase() {
    abstract fun searchResultDao(): SearchResultDao
}
