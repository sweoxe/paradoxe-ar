package com.paradoxe.arobjectexplorer.presentation.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import kotlin.random.Random

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
        color = Color(0xFF010101)
    ) {
        if (permissionState.allPermissionsGranted) {
            ArCameraView(viewModel, onNavigateToSettings)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SYSTEM ERROR: CAMERA ACCESS DENIED", color = Color(0xFFFF0066), fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { permissionState.launchMultiplePermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FBFF))) {
                        Text("INITIALIZE ACCESS", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ArCameraView(
    viewModel: ArViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedObject by remember { mutableStateOf<DetectedArObject?>(null) }
    var fullScreenInfo by remember { mutableStateOf<ScannedObject?>(null) }

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build().also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            viewModel.processImage(image)
                        }
                        imageProxy.close()
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        ArHolographicOverlay(
            trackedObjects = uiState.trackedObjects,
            imageWidth = uiState.imageWidth,
            imageHeight = uiState.imageHeight,
            onObjectClick = { selectedObject = it },
            onInfoClick = { fullScreenInfo = it.info }
        )

        ScanLinesOverlay()
        LensVignette()
        HiltHudDisplay()

        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(20.dp)
                .background(Color.Black.copy(0.7f), CircleShape).border(1.dp, Color(0xFF00FBFF).copy(0.4f), CircleShape)
        ) { Icon(Icons.Default.Settings, null, tint = Color(0xFF00FBFF)) }

        AnimatedVisibility(
            visible = selectedObject != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedObject?.let { obj ->
                InfoCard(obj = obj, onDismiss = { selectedObject = null }, onFullDetail = { fullScreenInfo = obj.info })
            }
        }

        if (fullScreenInfo != null) {
            FullScreenDetail(info = fullScreenInfo!!, onDismiss = { fullScreenInfo = null })
        }
    }
}

@Composable
fun HiltHudDisplay() {
    val infiniteTransition = rememberInfiniteTransition(label = "hud")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "a"
    )

    Box(modifier = Modifier.fillMaxSize().padding(20.dp).windowInsetsPadding(WindowInsets.safeDrawing)) {
        HudCorner(Alignment.TopStart)
        HudCorner(Alignment.TopEnd)
        HudCorner(Alignment.BottomStart)
        HudCorner(Alignment.BottomEnd)
        
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 100.dp)) {
            Text("CORE_LINK: STABLE", color = Color(0xFF00FBFF).copy(alpha), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            Text("LATENCY: ${Random.nextInt(12, 18)}MS", color = Color(0xFF00FBFF).copy(alpha * 0.7f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            Text("TRACKING: ACTIVE [MAX:03]", color = Color(0xFF00FBFF).copy(alpha), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        }

        Box(modifier = Modifier.align(Alignment.Center).size(40.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val color = Color(0xFF00FBFF).copy(0.2f)
                drawLine(color, Offset(size.width/2, 0f), Offset(size.width/2, 10f), 1.dp.toPx())
                drawLine(color, Offset(size.width/2, size.height), Offset(size.width/2, size.height-10f), 1.dp.toPx())
                drawLine(color, Offset(0f, size.height/2), Offset(10f, size.height/2), 1.dp.toPx())
                drawLine(color, Offset(size.width, size.height/2), Offset(size.width-10f, size.height/2), 1.dp.toPx())
            }
        }
    }
}

@Composable
fun HudCorner(alignment: Alignment) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val s = size.width
        val color = Color(0xFF00FBFF).copy(0.4f)
        val t = 2.dp.toPx()
        val l = 8.dp.toPx()
        when(alignment) {
            Alignment.TopStart -> { drawLine(color, Offset(0f,0f), Offset(l,0f), t); drawLine(color, Offset(0f,0f), Offset(0f,l), t) }
            Alignment.TopEnd -> { drawLine(color, Offset(s,0f), Offset(s-l,0f), t); drawLine(color, Offset(s,0f), Offset(s,l), t) }
            Alignment.BottomStart -> { drawLine(color, Offset(0f,s), Offset(l,s), t); drawLine(color, Offset(0f,s), Offset(0f,s-l), t) }
            Alignment.BottomEnd -> { drawLine(color, Offset(s,s), Offset(s-l,s), t); drawLine(color, Offset(s,s), Offset(s,s-l), t) }
        }
    }
}

@Composable
fun ScanLinesOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanOffset by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "s")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 8.dp.toPx()
        for (y in 0 until size.height.toInt() step spacing.toInt()) {
            drawLine(Color(0xFF00FBFF).copy(0.03f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
        }
        val scanY = scanOffset * size.height
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF00FBFF).copy(0.1f), Color.Transparent), scanY - 50f, scanY + 50f), Offset(0f, scanY - 50f), Size(size.width, 100f))
    }
}

@Composable
fun LensVignette() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(0.6f)), center, size.maxDimension/1.5f))
    }
}

@Composable
fun ArHolographicOverlay(trackedObjects: List<DetectedArObject>, imageWidth: Int, imageHeight: Int, onObjectClick: (DetectedArObject) -> Unit, onInfoClick: (DetectedArObject) -> Unit) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scaleX = constraints.maxWidth.toFloat() / imageWidth
        val scaleY = constraints.maxHeight.toFloat() / imageHeight
        Canvas(modifier = Modifier.fillMaxSize()) {
            trackedObjects.forEach { obj ->
                val r = obj.boundingBox
                drawHolographicFrame(r.left * scaleX, r.top * scaleY, r.width() * scaleX, r.height() * scaleY, Color(0xFF00FBFF), obj.info?.name ?: obj.labels.firstOrNull() ?: "ID...", obj.isLoading)
            }
        }
        trackedObjects.forEach { obj ->
            val r = obj.boundingBox
            Box(modifier = Modifier.offset((r.left * scaleX / density.density).dp, (r.top * scaleY / density.density).dp).size((r.width() * scaleX / density.density).dp, (r.height() * scaleY / density.density).dp).clickable { onObjectClick(obj) })
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHolographicFrame(left: Float, top: Float, width: Float, height: Float, color: Color, label: String, isLoading: Boolean) {
    val t = 2.dp.toPx(); val cl = 20.dp.toPx(); val c = if (isLoading) color.copy(0.4f) else color
    val path = Path().apply {
        moveTo(left, top + cl); lineTo(left, top); lineTo(left + cl, top)
        moveTo(left + width - cl, top); lineTo(left + width, top); lineTo(left + width, top + cl)
        moveTo(left + width, top + height - cl); lineTo(left + width, top + height); lineTo(left + width - cl, top + height)
        moveTo(left + cl, top + height); lineTo(left, top + height); lineTo(left, top + height - cl)
    }
    drawPath(path, c, style = Stroke(t))
    drawRect(c.copy(0.05f), Offset(left, top), Size(width, height))
    val p = android.graphics.Paint().apply { this.color = c.toArgb(); textSize = 32f; typeface = android.graphics.Typeface.MONOSPACE; isFakeBoldText = true }
    val tw = p.measureText(label.uppercase())
    drawRect(Color.Black.copy(0.7f), Offset(left, top - 45f), Size(tw + 20f, 40f))
    drawContext.canvas.nativeCanvas.drawText(label.uppercase(), left + 10f, top - 15f, p)
}

@Composable
fun InfoCard(obj: DetectedArObject, onDismiss: () -> Unit, onFullDetail: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp)).background(Color.Black.copy(0.9f)).border(1.dp, Color(0xFF00FBFF).copy(0.4f), RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(obj.info?.name?.uppercase() ?: obj.labels.firstOrNull()?.uppercase() ?: "SCAN...", color = Color(0xFF00FBFF), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White.copy(0.5f)) }
            }
            Spacer(Modifier.height(12.dp))
            Text(obj.info?.description?.take(180) ?: "PROCESSING_STREAM... CONNECTING_TO_WIKIPEDIA_NODES...", color = Color.White.copy(0.8f), fontSize = 13.sp)
            if (obj.info != null) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onFullDetail,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FBFF).copy(0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00FBFF).copy(0.4f))
                    ) {
                        Text("DATA_STREAM", color = Color(0xFF00FBFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${obj.info.name}"))) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.2f))
                    ) {
                        Text("SEARCH", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenDetail(info: ScannedObject, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("CLASSIFICATION: ${info.name.uppercase()}", color = Color(0xFF00FBFF), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            Text(info.name, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    }
                    Spacer(Modifier.height(24.dp))
                    if (info.thumbnailUrl != null) {
                        AsyncImage(model = info.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(32.dp)).border(1.dp, Color(0xFF00FBFF).copy(0.3f), RoundedCornerShape(32.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.height(24.dp))
                    }
                    HorizontalDivider(color = Color(0xFF00FBFF).copy(0.2f))
                    Text(info.description, color = Color.White.copy(0.9f), style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp, modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

fun Color.toArgb() = (alpha * 255.0f + 0.5f).toInt() shl 24 or (red * 255.0f + 0.5f).toInt() shl 16 or (green * 255.0f + 0.5f).toInt() shl 8 or (blue * 255.0f + 0.5f).toInt()
