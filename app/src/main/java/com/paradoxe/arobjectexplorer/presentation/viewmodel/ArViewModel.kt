package com.paradoxe.arobjectexplorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxe.arobjectexplorer.data.local.SettingsManager
import com.paradoxe.arobjectexplorer.domain.models.ScannedObject
import com.paradoxe.arobjectexplorer.domain.repository.ObjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArUiState(
    val detectedObject: ScannedObject? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateInterval: Long = 2500L,
    val lang: String = "ru",
    val iamToken: String = "",
    val folderId: String = ""
)

@HiltViewModel
class ArViewModel @Inject constructor(
    private val repository: ObjectRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsManager.iamToken,
                settingsManager.folderId,
                settingsManager.updateInterval
            ) { token, folder, interval ->
                Triple(token, folder, interval)
            }.collect { (token, folder, interval) ->
                _uiState.update { it.copy(
                    iamToken = token,
                    folderId = folder,
                    updateInterval = interval
                ) }
            }
        }
    }

    fun onProcessFrame(imageBase64: String, imageHash: String) {
        val state = _uiState.value
        if (state.isLoading || state.iamToken.isEmpty() || state.folderId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.identifyObject(
                imageBase64 = imageBase64,
                imageHash = imageHash,
                iamToken = state.iamToken,
                folderId = state.folderId,
                lang = state.lang
            ).onSuccess { scannedObject ->
                _uiState.update { it.copy(detectedObject = scannedObject, isLoading = false) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun clearDetection() {
        _uiState.update { it.copy(detectedObject = null) }
    }
}
