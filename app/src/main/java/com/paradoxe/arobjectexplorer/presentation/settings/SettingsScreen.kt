package com.paradoxe.arobjectexplorer.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val savedWikiLang by viewModel.wikiLang.collectAsState()
    val savedInterval by viewModel.updateInterval.collectAsState()

    var wikiLang by remember(savedWikiLang) { mutableStateOf(savedWikiLang) }
    var updateInterval by remember(savedInterval) { mutableStateOf(savedInterval) }

    Scaffold(
        containerColor = Color(0xFF050505),
        topBar = {
            TopAppBar(
                title = { Text("SYSTEM_CONFIG", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color(0xFF00FBFF)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FBFF))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            Text("DATA_SOURCE_LANGUAGE", color = Color(0xFF00FBFF).copy(0.6f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("ru" to "РУССКИЙ", "en" to "ENGLISH").forEach { (code, name) ->
                    OutlinedButton(
                        onClick = { wikiLang = code },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (wikiLang == code) Color(0xFF00FBFF).copy(0.1f) else Color.Transparent,
                            contentColor = if (wikiLang == code) Color(0xFF00FBFF) else Color.White.copy(0.6f)
                        ),
                        border = BorderStroke(1.dp, if (wikiLang == code) Color(0xFF00FBFF) else Color.White.copy(0.2f))
                    ) {
                        Text(name, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text("SCAN_FREQUENCY: ${"%.1f".format(updateInterval)}s", color = Color(0xFF00FBFF).copy(0.6f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            Slider(
                value = updateInterval,
                onValueChange = { updateInterval = it },
                valueRange = 1f..5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00FBFF),
                    activeTrackColor = Color(0xFF00FBFF),
                    inactiveTrackColor = Color.White.copy(0.1f)
                )
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { 
                    viewModel.saveSettings(wikiLang, updateInterval)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FBFF)),
            ) {
                Text("SAVE_CHANGES", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

