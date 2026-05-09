package com.paradoxe.arobjectexplorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val lang: String = "ru"
)

@HiltViewModel
class ArViewModel @Inject constructor(
    private val repository: ObjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    fun onProcessFrame(imageBase64: String, imageHash: String, iamToken: String, folderId: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.identifyObject(
                imageBase64 = imageBase64,
                imageHash = imageHash,
                iamToken = iamToken,
                folderId = folderId,
                lang = _uiState.value.lang
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
