package com.paradoxe.arobjectexplorer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxe.arobjectexplorer.data.local.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val iamToken = settingsManager.iamToken.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val folderId = settingsManager.folderId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val updateInterval = settingsManager.updateInterval.map { (it / 1000f) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.5f)

    fun saveSettings(token: String, folder: String, interval: Float) {
        viewModelScope.launch {
            settingsManager.saveSettings(token, folder, interval)
        }
    }
}
