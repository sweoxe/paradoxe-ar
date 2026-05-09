package com.paradoxe.arobjectexplorer.presentation.viewmodel

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.paradoxe.arobjectexplorer.data.local.SettingsManager
import com.paradoxe.arobjectexplorer.domain.models.ScannedObject
import com.paradoxe.arobjectexplorer.domain.repository.ObjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class DetectedArObject(
    val id: Int,
    val boundingBox: Rect,
    val labels: List<String>,
    val info: ScannedObject? = null,
    val isLoading: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

data class ArUiState(
    val trackedObjects: List<DetectedArObject> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val wikiLang: String = "ru",
    val imageWidth: Int = 480,
    val imageHeight: Int = 640
)

@HiltViewModel
class ArViewModel @Inject constructor(
    private val repository: ObjectRepository,
    private val detector: ObjectDetector,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    init {
        settingsManager.wikiLang.onEach { lang ->
            _uiState.update { it.copy(wikiLang = lang) }
        }.launchIn(viewModelScope)
    }

    fun processImage(image: InputImage, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val results = detector.process(image).await()
                onComplete() // Close early if possible, but after await
                
                val imgWidth = image.width
                val imgHeight = image.height
                
                _uiState.update { it.copy(imageWidth = imgWidth, imageHeight = imgHeight) }
                
                // 1. Filter: Up to 3 objects, preferring those already tracked
                val currentTrackedIds = _uiState.value.trackedObjects.map { it.id }.toSet()
                
                // Sort results: existing objects first, then by size
                val sortedResults = results.sortedWith(compareByDescending<DetectedObject> { 
                    currentTrackedIds.contains(it.trackingId ?: -1) 
                }.thenByDescending { 
                    it.boundingBox.width() * it.boundingBox.height() 
                })

                val top3 = sortedResults.take(3)

                val currentObjects = top3.map { mlObject ->
                    val id = mlObject.trackingId ?: mlObject.hashCode()
                    val existing = _uiState.value.trackedObjects.find { it.id == id }
                    
                    DetectedArObject(
                        id = id,
                        boundingBox = mlObject.boundingBox,
                        labels = mlObject.labels.map { it.text },
                        info = existing?.info,
                        isLoading = existing?.isLoading ?: false,
                        lastSeen = System.currentTimeMillis()
                    )
                }

                _uiState.update { it.copy(trackedObjects = currentObjects) }

                // Automatic info fetch
                currentObjects.forEach { obj ->
                    if (obj.info == null && !obj.isLoading && obj.labels.isNotEmpty()) {
                        fetchObjectInfo(obj.id, obj.labels.first())
                    }
                }
            } catch (e: Exception) {
                onComplete()
                // Analysis errors should not stop the flow
            }
        }
    }

    private fun fetchObjectInfo(objectId: Int, label: String) {
        viewModelScope.launch {
            updateObjectLoading(objectId, true)
            // Пытаемся получить на выбранном языке, если не выйдет - можно добавить fallback
            repository.getObjectInfo(label, _uiState.value.wikiLang)
                .onSuccess { info ->
                    updateObjectInfo(objectId, info)
                }
                .onFailure {
                    // Fallback to English if current is not English
                    if (_uiState.value.wikiLang != "en") {
                        repository.getObjectInfo(label, "en").onSuccess { enInfo ->
                            updateObjectInfo(objectId, enInfo)
                        }.onFailure {
                            updateObjectLoading(objectId, false)
                        }
                    } else {
                        updateObjectLoading(objectId, false)
                    }
                }
        }
    }

    private fun updateObjectLoading(objectId: Int, isLoading: Boolean) {
        _uiState.update { state ->
            state.copy(
                trackedObjects = state.trackedObjects.map {
                    if (it.id == objectId) it.copy(isLoading = isLoading) else it
                }
            )
        }
    }

    private fun updateObjectInfo(objectId: Int, info: ScannedObject) {
        _uiState.update { state ->
            state.copy(
                trackedObjects = state.trackedObjects.map {
                    if (it.id == objectId) it.copy(info = info, isLoading = false) else it
                }
            )
        }
    }
}
