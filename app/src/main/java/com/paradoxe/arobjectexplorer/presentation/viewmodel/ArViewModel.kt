package com.paradoxe.arobjectexplorer.presentation.viewmodel

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
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
    val lang: String = "ru",
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

    fun processImage(image: InputImage) {
        viewModelScope.launch {
            try {
                val results = detector.process(image).await()
                val imgWidth = image.width
                val imgHeight = image.height
                
                // Update dimensions for UI scaling
                _uiState.update { it.copy(imageWidth = imgWidth, imageHeight = imgHeight) }
                
                // 1. Filter out-of-frame objects (at least 10% from edges)
                val marginX = imgWidth * 0.05f
                val marginY = imgHeight * 0.05f
                val filteredResults = results.filter { mlObject ->
                    val b = mlObject.boundingBox
                    b.left > marginX && b.top > marginY && b.right < (imgWidth - marginX) && b.bottom < (imgHeight - marginY)
                }

                val currentObjects = filteredResults.map { mlObject ->
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
                }.take(3) // Limit to 3 objects

                // Keep tracked objects that were seen recently (simple persistence)
                val now = System.currentTimeMillis()
                val persistedObjects = _uiState.value.trackedObjects
                    .filter { it.info != null && (now - it.lastSeen < 2000) } // Keep if has info and seen recently
                
                // Merge current and persisted (prefer current for position)
                val mergedObjects = currentObjects.toMutableList()
                persistedObjects.forEach { pObj ->
                    if (mergedObjects.none { it.id == pObj.id }) {
                        // If not in current, but seen recently, we could keep it? 
                        // But for Bounding Box we need fresh coords. So maybe not.
                    }
                }

                _uiState.update { it.copy(trackedObjects = currentObjects) }

                // Fetch info for new objects or objects without info
                currentObjects.forEach { obj ->
                    if (obj.info == null && !obj.isLoading && obj.labels.isNotEmpty()) {
                        fetchObjectInfo(obj.id, obj.labels.first())
                    }
                }
            } catch (e: Exception) {
                // Silently handle analysis errors to keep preview smooth
            }
        }
    }

    private fun fetchObjectInfo(objectId: Int, label: String) {
        viewModelScope.launch {
            updateObjectLoading(objectId, true)
            repository.getObjectInfo(label, _uiState.value.lang)
                .onSuccess { info ->
                    updateObjectInfo(objectId, info)
                }
                .onFailure {
                    updateObjectLoading(objectId, false)
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
