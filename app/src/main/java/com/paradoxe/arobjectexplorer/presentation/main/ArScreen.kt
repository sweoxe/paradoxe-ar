package com.paradoxe.arobjectexplorer.presentation.main

import android.Manifest
import android.graphics.Rect
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
        color = Color(0xFF010101) // Ultra dark background
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
                        "SYSTEM ERROR: CAMERA ACCESS DENIED",
                        color = Color(0xFFFF0066),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { permissionState.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FBFF)),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("INITIALIZE ACCESS", color = Color.Black, fontWeight = FontWeight.Bold)
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
        
        // Lens Vignette
        LensVignette()

        // HUD Elements
        HiltHudDisplay()

        // Settings Button
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp)
                .background(Color.Black.copy(0.7f), CircleShape)
                .border(1.dp, Color(0xFF00FBFF).copy(0.4f), CircleShape)
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
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_anim"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 1.dp.toPx()
        val spacing = 6.dp.toPx()
        for (y in 0 until size.height.toInt() step spacing.toInt()) {
            drawLine(
                color = Color(0xFF00FBFF).copy(alpha = 0.05f),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = strokeWidth
            )
        }
        
        // Moving scan line
        val scanY = scanOffset * size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF00FBFF).copy(0.1f), Color.Transparent),
                startY = scanY - 50f,
                endY = scanY + 50f
            ),
            topLeft = Offset(0f, scanY - 50f),
            size = Size(size.width, 100f)
        )
    }
}

@Composable
fun LensVignette() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(0.6f)),
                center = center,
                radius = size.maxDimension / 1.5f
            )
        )
    }
}

@Composable
fun HiltHudDisplay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        // Corners HUD
        HudCorner(Alignment.TopStart)
        HudCorner(Alignment.TopEnd)
        HudCorner(Alignment.BottomStart)
        HudCorner(Alignment.BottomEnd)
        
        // Side text
        Text(
            "CORE_LINK: STABLE\nLATENCY: 14MS\nOBJECT_TRACKING: ACTIVE",
            color = Color(0xFF00FBFF).copy(0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 100.dp)
        )
    }
}

@Composable
fun HudCorner(alignment: Alignment) {
    Box(modifier = Modifier.size(30.dp), contentAlignment = alignment) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width
            val color = Color(0xFF00FBFF).copy(0.4f)
            val thickness = 2.dp.toPx()
            val len = 10.dp.toPx()
            
            when(alignment) {
                Alignment.TopStart -> {
                    drawLine(color, Offset(0f, 0f), Offset(len, 0f), thickness)
                    drawLine(color, Offset(0f, 0f), Offset(0f, len), thickness)
                }
                Alignment.TopEnd -> {
                    drawLine(color, Offset(s, 0f), Offset(s - len, 0f), thickness)
                    drawLine(color, Offset(s, 0f), Offset(s, len), thickness)
                }
                Alignment.BottomStart -> {
                    drawLine(color, Offset(0f, s), Offset(len, s), thickness)
                    drawLine(color, Offset(0f, s), Offset(0f, s - len), thickness)
                }
                Alignment.BottomEnd -> {
                    drawLine(color, Offset(s, s), Offset(s - len, s), thickness)
                    drawLine(color, Offset(s, s), Offset(s, s - len), thickness)
                }
            }
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

        // Scaling logic (ML Kit input size to screen size)
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

                drawHolographicFrame(
                    left = left,
                    top = top,
                    width = width,
                    height = height,
                    color = Color(0xFF00FBFF),
                    label = obj.info?.name ?: obj.labels.firstOrNull() ?: "IDENTIFYING...",
                    isLoading = obj.isLoading
                )
            }
        }

        // Interaction layers
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
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHolographicFrame(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    label: String,
    isLoading: Boolean
) {
    val strokeWidth = 2.dp.toPx()
    val cornerLen = 24.dp.toPx()
    val mainColor = if (isLoading) color.copy(0.4f) else color
    
    // Outer highlight box
    drawRect(
        color = mainColor.copy(0.05f),
        topLeft = Offset(left, top),
        size = Size(width, height)
    )

    // Advanced Corners
    val path = Path().apply {
        // TL
        moveTo(left, top + cornerLen)
        lineTo(left, top)
        lineTo(left + cornerLen, top)
        // TR
        moveTo(left + width - cornerLen, top)
        lineTo(left + width, top)
        lineTo(left + width, top + cornerLen)
        // BR
        moveTo(left + width, top + height - cornerLen)
        lineTo(left + width, top + height)
        lineTo(left + width - cornerLen, top + height)
        // BL
        moveTo(left + cornerLen, top + height)
        lineTo(left, top + height)
        lineTo(left, top + height - cornerLen)
    }
    
    drawPath(path, mainColor, style = Stroke(strokeWidth))

    // Label HUD
    val labelPaint = android.graphics.Paint().apply {
        this.color = mainColor.toArgb()
        textSize = 36f
        isFakeBoldText = true
        typeface = android.graphics.Typeface.MONOSPACE
        letterSpacing = 0.1f
    }
    
    val padding = 10f
    val textWidth = labelPaint.measureText(label.uppercase())
    
    // Label label bg
    drawRect(
        color = Color.Black.copy(0.6f),
        topLeft = Offset(left, top - 50f),
        size = Size(textWidth + 20f, 50f)
    )
    
    drawContext.canvas.nativeCanvas.drawText(
        label.uppercase(),
        left + 10f,
        top - 15f,
        labelPaint
    )
    
    // Animated circle for "scanning"
    if (isLoading) {
        drawCircle(
            color = color,
            radius = 5f,
            center = Offset(left + textWidth + 40f, top - 25f),
            alpha = 0.8f
        )
    }
}

@Composable
fun InfoCard(
    obj: DetectedArObject,
    onDismiss: () -> Unit,
    onFullDetail: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clip(RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp))
            .background(Color.Black.copy(0.95f))
            .border(1.dp, Color(0xFF00FBFF).copy(0.5f), RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    obj.info?.name?.uppercase() ?: obj.labels.firstOrNull()?.uppercase() ?: "SCANNING SOURCE...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF00FBFF),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(0.5f))
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                obj.info?.description?.take(160)?.let { if (it.length >= 160) "$it..." else it } 
                    ?: "DATA_EXTRACTION_IN_PROGRESS... ACCESSING GLOBAL INFORMATION REPOSITORY (WIKIPEDIA)... STABILIZING FEED.",
                color = Color.White.copy(0.85f),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )

            if (obj.info != null) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onFullDetail,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    border = border(width = 1.dp, color = Color(0xFF00FBFF).copy(0.6f)).border
                ) {
                    Text("OPEN FULL DATA STREAM", color = Color(0xFF00FBFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                .background(Color(0xFF050505))
                .drawWithContent {
                    drawContent()
                    // HUD lines
                    drawLine(Color(0xFF00FBFF).copy(0.1f), Offset(0f, 100f), Offset(size.width, 100f), 1f)
                }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                "CLASSIFICATION: ${info.name.uppercase()}",
                                color = Color(0xFF00FBFF),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                info.name,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    if (info.thumbnailUrl != null) {
                        AsyncImage(
                            model = info.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(RoundedCornerShape(bottomStart = 40.dp, topEnd = 40.dp))
                                .border(1.dp, Color(0xFF00FBFF).copy(0.4f), RoundedCornerShape(bottomStart = 40.dp, topEnd = 40.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(32.dp))
                    }

                    Text(
                        "ANALYSIS_SUMMARY",
                        color = Color(0xFF00FBFF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(color = Color(0xFF00FBFF).copy(0.3f), modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text(
                        info.description,
                        color = Color.White.copy(0.9f),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Normal
                    )
                    
                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}

fun Color.toArgb() = (alpha * 255.0f + 0.5f).toInt() shl 24 or
    (red * 255.0f + 0.5f).toInt() shl 16 or
    (green * 255.0f + 0.5f).toInt() shl 8 or
    (blue * 255.0f + 0.5f).toInt()
