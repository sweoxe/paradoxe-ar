package com.paradoxe.arobjectexplorer.presentation.main

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.paradoxe.arobjectexplorer.presentation.viewmodel.ArViewModel
import com.paradoxe.arobjectexplorer.utils.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArScreenContent(
    viewModel: ArViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA)
    )

    if (permissionState.allPermissionsGranted) {
        ArCameraView(viewModel, onNavigateToSettings)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Запросить разрешения")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArCameraView(
    viewModel: ArViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )

                // Loop for capturing frames
                scope.launch {
                    while (true) {
                        delay(uiState.updateInterval)
                        if (!uiState.isLoading) {
                            previewView.bitmap?.let { bitmap ->
                                val (base64, hash) = ImageUtils.compressAndEncodeToBase64(bitmap)
                                // Note: IAM token and Folder ID should be fetched from DataStore
                                viewModel.onProcessFrame(base64, hash, "YOUR_TOKEN", "YOUR_FOLDER_ID")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // UI Overlays
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }

        // Floating label for detected object
        uiState.detectedObject?.let { obj ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-100).dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                    .clickable { showBottomSheet = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(obj.name, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp))
        }

        if (showBottomSheet && uiState.detectedObject != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false }
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(uiState.detectedObject!!.name, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.detectedObject!!.description)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { /* Open WebView */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Подробнее в Википедии")
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
