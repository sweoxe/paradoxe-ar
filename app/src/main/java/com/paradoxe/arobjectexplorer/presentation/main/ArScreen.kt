package com.paradoxe.arobjectexplorer.presentation.main

import android.Manifest
import android.graphics.Rect
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.common.InputImage
import com.paradoxe.arobjectexplorer.domain.models.ScannedObject
import com.paradoxe.arobjectexplorer.presentation.viewmodel.ArViewModel
import com.paradoxe.arobjectexplorer.presentation.viewmodel.DetectedArObject
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ArScreenContent(
    viewModel: ArViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF050505) // Deep dark background
    ) {
        if (permissionState.allPermissionsGranted) {
            ArCameraView(viewModel, onNavigateToSettings)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "ДЛЯ РАБОТЫ ТРЕБУЕТСЯ ДОСТУП К КАМЕРЕ",
                        color = Color(0xFF00FBFF),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { permissionState.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FBFF))
                    ) {
                        Text("ПРЕДОСТАВИТЬ", color = Color.Black)
                    }
                }
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
    
    var selectedObject by remember { mutableStateOf<DetectedArObject?>(null) }
    var fullScreenInfo by remember { mutableStateOf<ScannedObject?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            viewModel.processImage(image)
                        }
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Holographic Overlay
        ArHolographicOverlay(
            trackedObjects = uiState.trackedObjects,
            imageWidth = uiState.imageWidth,
            imageHeight = uiState.imageHeight,
            onObjectClick = { selectedObject = it },
            onInfoClick = { fullScreenInfo = it.info }
        )

        // Futuristic Scan-lines effect
        ScanLinesOverlay()

        // Settings Button
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
                .background(Color.Black.copy(0.6f), CircleShape)
                .border(1.dp, Color(0xFF00FBFF).copy(0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF00FBFF))
        }

        // Bottom Info Card
        AnimatedVisibility(
            visible = selectedObject != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedObject?.let { obj ->
                InfoCard(
                    obj = obj,
                    onDismiss = { selectedObject = null },
                    onFullDetail = { fullScreenInfo = obj.info }
                )
            }
        }

        // Full Screen Detail View
        if (fullScreenInfo != null) {
            FullScreenDetail(
                info = fullScreenInfo!!,
                onDismiss = { fullScreenInfo = null }
            )
        }
    }
}

@Composable
fun ScanLinesOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 1.dp.toPx()
        val spacing = 4.dp.toPx()
        for (y in 0 until size.height.toInt() step spacing.toInt()) {
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun ArHolographicOverlay(
    trackedObjects: List<DetectedArObject>,
    imageWidth: Int,
    imageHeight: Int,
    onObjectClick: (DetectedArObject) -> Unit,
    onInfoClick: (DetectedArObject) -> Unit
) {
    val density = LocalDensity.current
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // ML Kit results are in image coordinates. 
        // We need to scale them to screen coordinates.
        val scaleX = screenWidth / imageWidth
        val scaleY = screenHeight / imageHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            trackedObjects.forEach { obj ->
                val rect = obj.boundingBox
                val left = rect.left * scaleX
                val top = rect.top * scaleY
                val right = rect.right * scaleX
                val bottom = rect.bottom * scaleY

                val width = right - left
                val height = bottom - top

                // Holographic Box
                drawHolographicBox(
                    left = left,
                    top = top,
                    width = width,
                    height = height,
                    color = Color(0xFF00FBFF),
                    label = obj.info?.name ?: obj.labels.firstOrNull() ?: "SCANNING..."
                )
            }
        }

        // Touch targets Overlay
        trackedObjects.forEach { obj ->
            val rect = obj.boundingBox
            val left = (rect.left * scaleX) / density.density
            val top = (rect.top * scaleY) / density.density
            val width = (rect.width() * scaleX) / density.density
            val height = (rect.height() * scaleY) / density.density

            Box(
                modifier = Modifier
                    .offset(x = left.dp, y = top.dp)
                    .size(width.dp, height.dp)
                    .clickable { onObjectClick(obj) }
            ) {
                if (obj.info != null) {
                    IconButton(
                        onClick = { onInfoClick(obj) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(Color(0xFF00FBFF).copy(0.2f), CircleShape)
                            .border(1.dp, Color(0xFF00FBFF), CircleShape)
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFF00FBFF), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHolographicBox(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    label: String
) {
    val glowColor = color.copy(alpha = 0.3f)
    val strokeWidth = 2.dp.toPx()
    val cornerLen = 20.dp.toPx()

    // Corners
    val path = Path().apply {
        // Top Left
        moveTo(left, top + cornerLen)
        lineTo(left, top)
        lineTo(left + cornerLen, top)
        
        // Top Right
        moveTo(left + width - cornerLen, top)
        lineTo(left + width, top)
        lineTo(left + width, top + cornerLen)

        // Bottom Right
        moveTo(left + width, top + height - cornerLen)
        lineTo(left + width, top + height)
        lineTo(left + width - cornerLen, top + height)

        // Bottom Left
        moveTo(left + cornerLen, top + height)
        lineTo(left, top + height)
        lineTo(left, top + height - cornerLen)
    }

    drawPath(path, color, style = Stroke(strokeWidth))
    
    // Fill background semi-transparent
    drawRect(
        color = color.copy(alpha = 0.05f),
        topLeft = Offset(left, top),
        size = Size(width, height)
    )

    // Suble glow
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(0.1f), Color.Transparent),
            center = Offset(left + width/2, top + height/2),
            radius = width
        ),
        topLeft = Offset(left - 20f, top - 20f),
        size = Size(width + 40f, height + 40f)
    )

    // Label
    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = 40f
        isFakeBoldText = true
        typeface = android.graphics.Typeface.MONOSPACE
    }
    drawContext.canvas.nativeCanvas.drawText(
        label.uppercase(),
        left,
        top - 10f,
        paint
    )
}

fun Color.toArgb() = (alpha * 255.0f + 0.5f).toInt() shl 24 or
    (red * 255.0f + 0.5f).toInt() shl 16 or
    (green * 255.0f + 0.5f).toInt() shl 8 or
    (blue * 255.0f + 0.5f).toInt()

@Composable
fun InfoCard(
    obj: DetectedArObject,
    onDismiss: () -> Unit,
    onFullDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color(0xFF00FBFF).copy(0.3f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.9f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    obj.info?.name?.uppercase() ?: obj.labels.firstOrNull()?.uppercase() ?: "UNKNOWN",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF00FBFF),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                obj.info?.description?.take(150)?.let { if (it.length >= 150) "$it..." else it } 
                    ?: "Анализ данных субъекта... Инициализация энциклопедического модуля Wikipedia.",
                color = Color.White.copy(0.8f),
                style = MaterialTheme.typography.bodySmall
            )

            if (obj.info != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onFullDetail,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FBFF).copy(0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = border(width = 1.dp, color = Color(0xFF00FBFF).copy(0.5f)).border
                ) {
                    Text("ПОЛНЫЙ ДОСТУП", color = Color(0xFF00FBFF), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FullScreenDetail(
    info: ScannedObject,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .drawWithContent {
                    drawContent()
                    // Scanning line effect
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF00FBFF).copy(0.2f), Color.Transparent),
                            startY = 0f,
                            endY = size.height
                        ),
                        alpha = 0.3f
                    )
                }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            info.name.uppercase(),
                            color = Color(0xFF00FBFF),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    if (info.thumbnailUrl != null) {
                        AsyncImage(
                            model = info.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFF00FBFF).copy(0.3f), RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    Text(
                        "DATA_STREAM_CONNECTED",
                        color = Color(0xFF00FBFF),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalDivider(color = Color(0xFF00FBFF).copy(0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        info.description,
                        color = Color.White.copy(0.9f),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 28.sp
                    )
                    
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}
