package com.paradoxe.arobjectexplorer.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val savedIamToken by viewModel.iamToken.collectAsState()
    val savedFolderId by viewModel.folderId.collectAsState()
    val savedInterval by viewModel.updateInterval.collectAsState()

    var iamToken by remember(savedIamToken) { mutableStateOf(savedIamToken) }
    var folderId by remember(savedFolderId) { mutableStateOf(savedFolderId) }
    var updateInterval by remember(savedInterval) { mutableStateOf(savedInterval) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Yandex Vision API", style = MaterialTheme.typography.titleMedium)
            TextField(
                value = iamToken,
                onValueChange = { iamToken = it },
                label = { Text("IAM Token") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = folderId,
                onValueChange = { folderId = it },
                label = { Text("Folder ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text("Частота обновления (сек): ${"%.1f".format(updateInterval)}", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = updateInterval,
                onValueChange = { updateInterval = it },
                valueRange = 1f..10f
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { 
                    viewModel.saveSettings(iamToken, folderId, updateInterval)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}
