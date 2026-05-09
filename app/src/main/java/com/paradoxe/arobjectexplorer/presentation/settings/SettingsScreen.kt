package com.paradoxe.arobjectexplorer.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var iamToken by remember { mutableStateOf("") }
    var folderId by remember { mutableStateOf("") }
    var updateInterval by remember { mutableStateOf(2.5f) }

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
                onClick = { /* Save to DataStore */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}
