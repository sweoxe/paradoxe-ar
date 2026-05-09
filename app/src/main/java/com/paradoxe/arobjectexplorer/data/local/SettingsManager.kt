package com.paradoxe.arobjectexplorer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val IAM_TOKEN = stringPreferencesKey("iam_token")
    private val FOLDER_ID = stringPreferencesKey("folder_id")
    private val UPDATE_INTERVAL = floatPreferencesKey("update_interval")

    val iamToken: Flow<String> = context.dataStore.data.map { it[IAM_TOKEN] ?: "" }
    val folderId: Flow<String> = context.dataStore.data.map { it[FOLDER_ID] ?: "" }
    val updateInterval: Flow<Long> = context.dataStore.data.map { (it[UPDATE_INTERVAL] ?: 2.5f).toLong() * 1000L }

    suspend fun saveSettings(token: String, folder: String, interval: Float) {
        context.dataStore.edit {
            it[IAM_TOKEN] = token
            it[FOLDER_ID] = folder
            it[UPDATE_INTERVAL] = interval
        }
    }
}
