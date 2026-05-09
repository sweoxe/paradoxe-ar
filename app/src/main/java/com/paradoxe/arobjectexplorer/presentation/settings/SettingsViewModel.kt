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

    val wikiLang = settingsManager.wikiLang.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ru")
    val updateInterval = settingsManager.updateInterval.map { (it / 1000f) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.5f)

    fun saveSettings(lang: String, interval: Float) {
        viewModelScope.launch {
            settingsManager.saveSettings(lang, interval)
        }
    }
}
