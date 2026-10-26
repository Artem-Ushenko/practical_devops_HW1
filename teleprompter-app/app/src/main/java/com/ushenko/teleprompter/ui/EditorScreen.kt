package com.ushenko.teleprompter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ushenko.teleprompter.PrompterSettings
import com.ushenko.teleprompter.ScriptStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    script: String,
    onScriptChange: (String) -> Unit,
    settings: PrompterSettings,
    onSettingsChange: (PrompterSettings) -> Unit,
    onRecord: () -> Unit,
    onFloatingPrompter: () -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val words = remember(script) { ScriptStats.wordCount(script) }
    val duration = remember(script) { ScriptStats.formatDuration(ScriptStats.estimatedSeconds(script)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Суфлер") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Налаштування")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = script,
                onValueChange = onScriptChange,
                label = { Text("Текст для запису") },
                placeholder = { Text("Вставте або наберіть сценарій…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "$words слів · ≈ $duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { onScriptChange("") }, enabled = script.isNotEmpty()) {
                    Icon(Icons.Filled.Delete, contentDescription = "Очистити текст")
                }
            }
            Button(
                onClick = onRecord,
                enabled = script.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Записати відео з суфлером")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onFloatingPrompter,
                enabled = script.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Суфлер поверх камери Pixel")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            SettingsPanel(settings, onSettingsChange, Modifier.padding(bottom = 32.dp))
        }
    }
}
